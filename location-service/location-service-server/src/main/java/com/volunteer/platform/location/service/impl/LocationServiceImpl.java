package com.volunteer.platform.location.service.impl;

import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.mq.DomainEventPublisher;
import com.volunteer.platform.mq.RabbitDomainEventNames;
import com.volunteer.platform.location.client.dto.CheckinDTO;
import com.volunteer.platform.location.client.dto.CheckinQrCodeDTO;
import com.volunteer.platform.location.client.dto.LocationDTO;
import com.volunteer.platform.location.client.dto.NearbyUserDTO;
import com.volunteer.platform.location.client.dto.UserRealtimeStatusDTO;
import com.volunteer.platform.location.dao.CheckinRecordDAO;
import com.volunteer.platform.location.dao.LocationRecordDAO;
import com.volunteer.platform.location.dao.UserRealtimeStatusDAO;
import com.volunteer.platform.location.entity.CheckinRecordDO;
import com.volunteer.platform.location.entity.LocationRecordDO;
import com.volunteer.platform.location.entity.UserRealtimeStatusDO;
import com.volunteer.platform.location.manager.GeoLocationManager;
import com.volunteer.platform.location.service.LocationService;
import com.volunteer.platform.schedule.client.api.ScheduleClient;
import com.volunteer.platform.schedule.client.dto.ScheduleAssignmentDTO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class LocationServiceImpl implements LocationService {

    private static final String QR_CODE_PREFIX = "VSP_CHECKIN:";

    private static final String QR_CODE_IMAGE_PREFIX = "data:image/png;base64,";

    private static final int QR_CODE_SIZE = 240;

    private static final String ONLINE_STATUS = "ONLINE";

    private static final String NORMAL_CHECKIN_STATUS = "NORMAL";

    private static final String PENDING_CHECKIN_STATUS = "PENDING";

    private final LocationRecordDAO locationRecordDAO;

    private final UserRealtimeStatusDAO userRealtimeStatusDAO;

    private final CheckinRecordDAO checkinRecordDAO;
    private final DomainEventPublisher domainEventPublisher;
    private final GeoLocationManager geoLocationManager;
    private final ScheduleClient scheduleClient;

    @Value("${volunteer.checkin.qr-secret:volunteer-checkin-secret}")
    private String qrSecret = "volunteer-checkin-secret";

    @Value("${volunteer.checkin.qr-expire-minutes:15}")
    private long qrExpireMinutes = 15L;

    public LocationServiceImpl(LocationRecordDAO locationRecordDAO,
                               UserRealtimeStatusDAO userRealtimeStatusDAO,
                               CheckinRecordDAO checkinRecordDAO,
                               DomainEventPublisher domainEventPublisher,
                               GeoLocationManager geoLocationManager) {
        this(locationRecordDAO, userRealtimeStatusDAO, checkinRecordDAO, domainEventPublisher, geoLocationManager,
            null);
    }

    @Autowired
    public LocationServiceImpl(LocationRecordDAO locationRecordDAO,
                               UserRealtimeStatusDAO userRealtimeStatusDAO,
                               CheckinRecordDAO checkinRecordDAO,
                               DomainEventPublisher domainEventPublisher,
                               GeoLocationManager geoLocationManager,
                               ScheduleClient scheduleClient) {
        this.locationRecordDAO = locationRecordDAO;
        this.userRealtimeStatusDAO = userRealtimeStatusDAO;
        this.checkinRecordDAO = checkinRecordDAO;
        this.domainEventPublisher = domainEventPublisher;
        this.geoLocationManager = geoLocationManager;
        this.scheduleClient = scheduleClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserRealtimeStatusDTO report(LocationDTO dto) {
        validateLocation(dto);
        LocalDateTime reportTime = dto.getLocationTime() == null ? LocalDateTime.now() : dto.getLocationTime();
        String status = dto.getStatus() == null ? ONLINE_STATUS : dto.getStatus();
        locationRecordDAO.insert(toLocationRecordDO(dto, reportTime, status));
        userRealtimeStatusDAO.upsert(toRealtimeStatusDO(dto, reportTime, status));
        geoLocationManager.save(dto.getActivityId(), dto.getUserId(), dto.getLongitude(), dto.getLatitude());
        return getStatus(dto.getActivityId(), dto.getUserId());
    }

    @Override
    public List<NearbyUserDTO> nearby(Long activityId, BigDecimal longitude, BigDecimal latitude, Integer radiusMeter) {
        if (activityId == null || longitude == null || latitude == null) {
            throw new BusinessException(400, "nearby query location is required");
        }
        if (radiusMeter == null || radiusMeter <= 0) {
            throw new BusinessException(400, "radius must be positive");
        }
        List<Long> geoUserIds = geoLocationManager.nearby(activityId, longitude, latitude, radiusMeter);
        if (!geoUserIds.isEmpty()) {
            return geoUserIds.stream()
                .map(userId -> userRealtimeStatusDAO.selectByUserId(activityId, userId))
                .filter(statusDO -> statusDO != null && !"OFFLINE".equals(statusDO.getStatus()))
                .map(this::toNearbyUserDTO)
                .toList();
        }
        return userRealtimeStatusDAO.selectNearby(activityId, longitude, latitude, radiusMeter).stream()
            .map(this::toNearbyUserDTO)
            .toList();
    }

    @Override
    public UserRealtimeStatusDTO getStatus(Long activityId, Long userId) {
        if (userId == null) {
            throw new BusinessException(400, "user id is required");
        }
        UserRealtimeStatusDO statusDO = userRealtimeStatusDAO.selectByUserId(activityId, userId);
        if (statusDO == null) {
            throw new BusinessException(404, "user realtime status not found");
        }
        return toRealtimeStatusDTO(statusDO);
    }

    @Override
    public CheckinQrCodeDTO generateCheckinQrCode(CheckinDTO dto) {
        validateCheckinQrTarget(dto);
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(qrExpireMinutes);
        String payloadAssignmentId = dto.getAssignmentId() == null ? "0" : String.valueOf(dto.getAssignmentId());
        String payload = payloadAssignmentId + "|" + dto.getActivityId() + "|" + dto.getPositionId() + "|"
            + dto.getCheckinType() + "|" + toEpochSecond(expireTime);
        String payloadText = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        CheckinQrCodeDTO qrCodeDTO = new CheckinQrCodeDTO();
        qrCodeDTO.setAssignmentId(dto.getAssignmentId());
        qrCodeDTO.setActivityId(dto.getActivityId());
        qrCodeDTO.setPositionId(dto.getPositionId());
        qrCodeDTO.setCheckinType(dto.getCheckinType());
        qrCodeDTO.setExpireTime(expireTime);
        String qrCodeToken = QR_CODE_PREFIX + payloadText + "." + sign(payloadText);
        qrCodeDTO.setQrCodeToken(qrCodeToken);
        qrCodeDTO.setQrCode(toQrCodeImage(qrCodeToken));
        return qrCodeDTO;
    }

    @Override
    public CheckinDTO checkin(CheckinDTO dto) {
        normalizeQrCode(dto);
        applyQrCodeTarget(dto);
        resolvePositionQrCodeAssignment(dto);
        validateCheckin(dto);
        validateQrCode(dto);
        validateDuplicateCheckin(dto);
        LocalDateTime checkinTime = dto.getCheckinTime() == null ? LocalDateTime.now() : dto.getCheckinTime();
        String checkinStatus = dto.getCheckinStatus() == null ? NORMAL_CHECKIN_STATUS : dto.getCheckinStatus();
        CheckinRecordDO checkinRecordDO = toCheckinRecordDO(dto, checkinTime, checkinStatus);
        try {
            checkinRecordDAO.insert(checkinRecordDO);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(409, "checkin already exists");
        }
        dto.setId(checkinRecordDO.getId());
        dto.setCheckinTime(checkinTime);
        dto.setCheckinStatus(checkinStatus);
        domainEventPublisher.publish(RabbitDomainEventNames.SALARY_TOPIC, RabbitDomainEventNames.CHECKIN_COMPLETED,
            "checkin-" + checkinRecordDO.getId(),
            Map.of("checkinId", checkinRecordDO.getId(), "assignmentId", dto.getAssignmentId(),
                "activityId", dto.getActivityId(), "userId", dto.getUserId(), "checkinType", dto.getCheckinType()));
        return dto;
    }

    @Override
    public CheckinDTO submitAsyncCheckin(CheckinDTO dto) {
        normalizeQrCode(dto);
        applyQrCodeTarget(dto);
        resolvePositionQrCodeAssignment(dto);
        validateCheckin(dto);
        validateQrCode(dto);
        dto.setCheckinStatus(PENDING_CHECKIN_STATUS);
        domainEventPublisher.publish(RabbitDomainEventNames.CHECKIN_TOPIC, RabbitDomainEventNames.CHECKIN_REQUESTED,
            buildCheckinEventKey(dto), dto);
        return dto;
    }

    @Override
    public List<CheckinDTO> listCheckins(Long activityId, Long userId) {
        if (activityId == null) {
            throw new BusinessException(400, "checkin query target is required");
        }
        return checkinRecordDAO.selectByActivityAndUser(activityId, userId).stream()
            .map(this::toCheckinDTO)
            .toList();
    }

    private void validateLocation(LocationDTO dto) {
        if (dto == null || dto.getActivityId() == null || dto.getUserId() == null
            || dto.getLongitude() == null || dto.getLatitude() == null) {
            throw new BusinessException(400, "location report is required");
        }
    }

    private void validateCheckin(CheckinDTO dto) {
        validateCheckinTarget(dto);
        if (dto.getUserId() == null) {
            throw new BusinessException(400, "checkin info is required");
        }
    }

    private void validateCheckinTarget(CheckinDTO dto) {
        if (dto == null || dto.getAssignmentId() == null || dto.getActivityId() == null
            || dto.getPositionId() == null || dto.getCheckinType() == null) {
            throw new BusinessException(400, "checkin info is required");
        }
    }

    private void validateCheckinQrTarget(CheckinDTO dto) {
        if (dto == null || dto.getActivityId() == null || dto.getPositionId() == null
            || dto.getCheckinType() == null) {
            throw new BusinessException(400, "checkin info is required");
        }
    }

    private void validateDuplicateCheckin(CheckinDTO dto) {
        if (checkinRecordDAO.countByAssignmentUserType(dto.getAssignmentId(), dto.getUserId(), dto.getCheckinType())
            > 0) {
            throw new BusinessException(409, "checkin already exists");
        }
    }

    private String buildCheckinEventKey(CheckinDTO dto) {
        return "checkin-request-" + dto.getActivityId() + "-" + dto.getAssignmentId() + "-" + dto.getUserId()
            + "-" + dto.getCheckinType();
    }

    private void validateQrCode(CheckinDTO dto) {
        if (dto.getQrCode() == null || dto.getQrCode().isBlank()) {
            return;
        }
        CheckinQrPayload payload = parseQrCode(dto.getQrCode());
        if (!isPositionQrCode(payload.assignmentId())
            && !Objects.equals(String.valueOf(dto.getAssignmentId()), payload.assignmentId())
            || !Objects.equals(String.valueOf(dto.getActivityId()), payload.activityId())
            || !Objects.equals(String.valueOf(dto.getPositionId()), payload.positionId())
            || !Objects.equals(dto.getCheckinType(), payload.checkinType())) {
            throw new BusinessException(400, "checkin qr code target mismatch");
        }
    }

    private void normalizeQrCode(CheckinDTO dto) {
        if (dto == null) {
            return;
        }
        if ((dto.getQrCode() == null || dto.getQrCode().isBlank())
            && dto.getQrCodeToken() != null && !dto.getQrCodeToken().isBlank()) {
            dto.setQrCode(dto.getQrCodeToken());
        }
    }

    private void applyQrCodeTarget(CheckinDTO dto) {
        if (dto == null || dto.getQrCode() == null || dto.getQrCode().isBlank()) {
            return;
        }
        CheckinQrPayload payload = parseQrCode(dto.getQrCode());
        if (dto.getAssignmentId() == null && !isPositionQrCode(payload.assignmentId())) {
            dto.setAssignmentId(Long.valueOf(payload.assignmentId()));
        }
        if (dto.getActivityId() == null) {
            dto.setActivityId(Long.valueOf(payload.activityId()));
        }
        if (dto.getPositionId() == null) {
            dto.setPositionId(Long.valueOf(payload.positionId()));
        }
        if (dto.getCheckinType() == null || dto.getCheckinType().isBlank()) {
            dto.setCheckinType(payload.checkinType());
        }
    }

    private void resolvePositionQrCodeAssignment(CheckinDTO dto) {
        if (dto == null || dto.getAssignmentId() != null || dto.getUserId() == null || dto.getActivityId() == null
            || dto.getPositionId() == null) {
            return;
        }
        if (scheduleClient == null) {
            throw new BusinessException(500, "schedule client is required");
        }
        Result<List<ScheduleAssignmentDTO>> result = scheduleClient.listUserAssignments(dto.getUserId());
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BusinessException(404, "schedule assignment not found for checkin");
        }
        ScheduleAssignmentDTO assignmentDTO = result.getData().stream()
            .filter(assignment -> Objects.equals(dto.getActivityId(), assignment.getActivityId()))
            .filter(assignment -> Objects.equals(dto.getPositionId(), assignment.getPositionId()))
            .filter(assignment -> !"CANCELLED".equals(assignment.getAssignmentStatus()))
            .findFirst()
            .orElseThrow(() -> new BusinessException(404, "schedule assignment not found for checkin"));
        dto.setAssignmentId(assignmentDTO.getId());
    }

    private boolean isPositionQrCode(String assignmentId) {
        return "0".equals(assignmentId);
    }

    private CheckinQrPayload parseQrCode(String qrCode) {
        if (qrCode == null || !qrCode.startsWith(QR_CODE_PREFIX)) {
            throw new BusinessException(400, "checkin qr code is invalid");
        }
        String[] parts = qrCode.substring(QR_CODE_PREFIX.length()).split("\\.");
        if (parts.length != 2 || !Objects.equals(sign(parts[0]), parts[1])) {
            throw new BusinessException(400, "checkin qr code is invalid");
        }
        String[] values = decodeQrCodePayload(parts[0]);
        if (values.length != 5) {
            throw new BusinessException(400, "checkin qr code is invalid");
        }
        if (parseExpireSecond(values[4]) < toEpochSecond(LocalDateTime.now())) {
            throw new BusinessException(400, "checkin qr code expired");
        }
        return new CheckinQrPayload(values[0], values[1], values[2], values[3]);
    }

    private String[] decodeQrCodePayload(String payloadText) {
        try {
            String payload = new String(Base64.getUrlDecoder().decode(payloadText), StandardCharsets.UTF_8);
            return payload.split("\\|");
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "checkin qr code is invalid");
        }
    }

    private long parseExpireSecond(String expireSecond) {
        try {
            return Long.parseLong(expireSecond);
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "checkin qr code is invalid");
        }
    }

    private long toEpochSecond(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private String sign(String payloadText) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(qrSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payloadText.getBytes(
                StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException(500, "checkin qr code sign failed");
        }
    }

    private String toQrCodeImage(String qrCodeToken) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter().encode(qrCodeToken, BarcodeFormat.QR_CODE,
                QR_CODE_SIZE, QR_CODE_SIZE, hints);
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return QR_CODE_IMAGE_PREFIX + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (WriterException | java.io.IOException e) {
            throw new BusinessException(500, "checkin qr code image generate failed");
        }
    }

    private record CheckinQrPayload(String assignmentId, String activityId, String positionId, String checkinType) {
    }

    private LocationRecordDO toLocationRecordDO(LocationDTO dto, LocalDateTime reportTime, String status) {
        LocationRecordDO recordDO = new LocationRecordDO();
        recordDO.setActivityId(dto.getActivityId());
        recordDO.setUserId(dto.getUserId());
        recordDO.setLongitude(dto.getLongitude());
        recordDO.setLatitude(dto.getLatitude());
        recordDO.setLocationTime(reportTime);
        recordDO.setStatus(status);
        return recordDO;
    }

    private UserRealtimeStatusDO toRealtimeStatusDO(LocationDTO dto, LocalDateTime reportTime, String status) {
        UserRealtimeStatusDO statusDO = new UserRealtimeStatusDO();
        statusDO.setActivityId(dto.getActivityId());
        statusDO.setUserId(dto.getUserId());
        statusDO.setStatus(status);
        statusDO.setLongitude(dto.getLongitude());
        statusDO.setLatitude(dto.getLatitude());
        statusDO.setLastReportTime(reportTime);
        return statusDO;
    }

    private CheckinRecordDO toCheckinRecordDO(CheckinDTO dto, LocalDateTime checkinTime, String checkinStatus) {
        CheckinRecordDO recordDO = new CheckinRecordDO();
        recordDO.setAssignmentId(dto.getAssignmentId());
        recordDO.setActivityId(dto.getActivityId());
        recordDO.setPositionId(dto.getPositionId());
        recordDO.setUserId(dto.getUserId());
        recordDO.setCheckinType(dto.getCheckinType());
        recordDO.setCheckinStatus(checkinStatus);
        recordDO.setCheckinTime(checkinTime);
        recordDO.setLongitude(dto.getLongitude());
        recordDO.setLatitude(dto.getLatitude());
        recordDO.setQrCode(dto.getQrCode());
        return recordDO;
    }

    private CheckinDTO toCheckinDTO(CheckinRecordDO recordDO) {
        CheckinDTO dto = new CheckinDTO();
        dto.setId(recordDO.getId());
        dto.setAssignmentId(recordDO.getAssignmentId());
        dto.setActivityId(recordDO.getActivityId());
        dto.setPositionId(recordDO.getPositionId());
        dto.setUserId(recordDO.getUserId());
        dto.setCheckinType(recordDO.getCheckinType());
        dto.setCheckinStatus(recordDO.getCheckinStatus());
        dto.setCheckinTime(recordDO.getCheckinTime());
        dto.setLongitude(recordDO.getLongitude());
        dto.setLatitude(recordDO.getLatitude());
        dto.setQrCode(recordDO.getQrCode());
        return dto;
    }

    private UserRealtimeStatusDTO toRealtimeStatusDTO(UserRealtimeStatusDO statusDO) {
        UserRealtimeStatusDTO dto = new UserRealtimeStatusDTO();
        dto.setActivityId(statusDO.getActivityId());
        dto.setUserId(statusDO.getUserId());
        dto.setStatus(statusDO.getStatus());
        dto.setLongitude(statusDO.getLongitude());
        dto.setLatitude(statusDO.getLatitude());
        dto.setLastReportTime(statusDO.getLastReportTime());
        return dto;
    }

    private NearbyUserDTO toNearbyUserDTO(UserRealtimeStatusDO statusDO) {
        NearbyUserDTO dto = new NearbyUserDTO();
        dto.setUserId(statusDO.getUserId());
        dto.setLongitude(statusDO.getLongitude());
        dto.setLatitude(statusDO.getLatitude());
        dto.setDistanceMeter(statusDO.getDistanceMeter());
        dto.setStatus(statusDO.getStatus());
        dto.setLastReportTime(statusDO.getLastReportTime());
        return dto;
    }
}
