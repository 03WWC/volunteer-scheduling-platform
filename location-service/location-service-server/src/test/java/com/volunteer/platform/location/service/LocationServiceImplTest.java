package com.volunteer.platform.location.service;

import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.mq.DomainEventPublisher;
import com.volunteer.platform.location.client.dto.CheckinDTO;
import com.volunteer.platform.location.client.dto.CheckinQrCodeDTO;
import com.volunteer.platform.location.client.dto.LocationDTO;
import com.volunteer.platform.schedule.client.api.ScheduleClient;
import com.volunteer.platform.schedule.client.dto.ScheduleAssignmentDTO;
import com.volunteer.platform.schedule.client.dto.ScheduleDTO;
import com.volunteer.platform.schedule.client.dto.SupplementScheduleAssignmentDTO;
import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.location.dao.CheckinRecordDAO;
import com.volunteer.platform.location.dao.LocationRecordDAO;
import com.volunteer.platform.location.dao.UserRealtimeStatusDAO;
import com.volunteer.platform.location.entity.CheckinRecordDO;
import com.volunteer.platform.location.entity.LocationRecordDO;
import com.volunteer.platform.location.entity.UserRealtimeStatusDO;
import com.volunteer.platform.location.manager.GeoLocationManager;
import com.volunteer.platform.location.service.impl.LocationServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationServiceImplTest {

    @Test
    void reportsLocationAndRefreshesRealtimeStatus() {
        InMemoryUserRealtimeStatusDAO statusDAO = new InMemoryUserRealtimeStatusDAO();
        InMemoryGeoLocationManager geoLocationManager = new InMemoryGeoLocationManager();
        LocationService service = new LocationServiceImpl(new InMemoryLocationRecordDAO(), statusDAO,
            new InMemoryCheckinRecordDAO(), new RecordingDomainEventPublisher(), geoLocationManager);

        service.report(createLocationDTO(101L, "ONLINE"));

        assertThat(service.getStatus(100L, 101L).getStatus()).isEqualTo("ONLINE");
        assertThat(statusDAO.statuses).hasSize(1);
        assertThat(geoLocationManager.savedUserIds).containsExactly(101L);
    }

    @Test
    void nearbyPrefersGeoUsersAndHydratesRealtimeStatus() {
        InMemoryUserRealtimeStatusDAO statusDAO = new InMemoryUserRealtimeStatusDAO();
        InMemoryGeoLocationManager geoLocationManager = new InMemoryGeoLocationManager();
        LocationService service = new LocationServiceImpl(new InMemoryLocationRecordDAO(), statusDAO,
            new InMemoryCheckinRecordDAO(), new RecordingDomainEventPublisher(), geoLocationManager);
        service.report(createLocationDTO(101L, "ONLINE"));
        LocationDTO farLocationDTO = createLocationDTO(102L, "ONLINE");
        farLocationDTO.setLongitude(new BigDecimal("120.0000000"));
        farLocationDTO.setLatitude(new BigDecimal("30.0000000"));
        service.report(farLocationDTO);
        geoLocationManager.nearbyUserIds = List.of(102L, 101L);

        assertThat(service.nearby(100L, new BigDecimal("113.0000000"), new BigDecimal("23.0000000"), 1000))
            .extracting(nearbyUser -> nearbyUser.getUserId())
            .containsExactly(102L, 101L);
    }

    @Test
    void nearbyFallsBackToMysqlWhenGeoHasNoUsers() {
        InMemoryUserRealtimeStatusDAO statusDAO = new InMemoryUserRealtimeStatusDAO();
        InMemoryGeoLocationManager geoLocationManager = new InMemoryGeoLocationManager();
        LocationService service = new LocationServiceImpl(new InMemoryLocationRecordDAO(), statusDAO,
            new InMemoryCheckinRecordDAO(), new RecordingDomainEventPublisher(), geoLocationManager);
        service.report(createLocationDTO(101L, "ONLINE"));
        LocationDTO farLocationDTO = createLocationDTO(102L, "ONLINE");
        farLocationDTO.setLongitude(new BigDecimal("120.0000000"));
        farLocationDTO.setLatitude(new BigDecimal("30.0000000"));
        service.report(farLocationDTO);
        geoLocationManager.nearbyUserIds = List.of();

        assertThat(service.nearby(100L, new BigDecimal("113.0000000"), new BigDecimal("23.0000000"), 1000))
            .extracting(nearbyUser -> nearbyUser.getUserId())
            .containsExactly(101L);
    }

    @Test
    void checkinCreatesRecordAndDefaultsStatus() {
        RecordingDomainEventPublisher eventPublisher = new RecordingDomainEventPublisher();
        InMemoryCheckinRecordDAO checkinRecordDAO = new InMemoryCheckinRecordDAO();
        LocationService service = new LocationServiceImpl(new InMemoryLocationRecordDAO(),
            new InMemoryUserRealtimeStatusDAO(), checkinRecordDAO, eventPublisher,
            new InMemoryGeoLocationManager());

        CheckinDTO checkinDTO = service.checkin(createCheckinDTO());

        assertThat(checkinDTO.getId()).isEqualTo(1L);
        assertThat(checkinDTO.getCheckinStatus()).isEqualTo("NORMAL");
        assertThat(eventPublisher.events).extracting(event -> event.topic())
            .containsExactly("salary-topic");
        assertThat(eventPublisher.events).extracting(event -> event.eventType())
            .containsExactly("CHECKIN_COMPLETED");
        assertThat(service.listCheckins(100L, 101L)).hasSize(1);
    }

    @Test
    void asyncCheckinPublishesRequestWithoutCreatingRecordImmediately() {
        RecordingDomainEventPublisher eventPublisher = new RecordingDomainEventPublisher();
        InMemoryCheckinRecordDAO checkinRecordDAO = new InMemoryCheckinRecordDAO();
        LocationService service = new LocationServiceImpl(new InMemoryLocationRecordDAO(),
            new InMemoryUserRealtimeStatusDAO(), checkinRecordDAO, eventPublisher,
            new InMemoryGeoLocationManager());
        CheckinDTO checkinDTO = createCheckinDTO();

        CheckinDTO acceptedDTO = service.submitAsyncCheckin(checkinDTO);

        assertThat(acceptedDTO.getCheckinStatus()).isEqualTo("PENDING");
        assertThat(service.listCheckins(100L, 101L)).isEmpty();
        assertThat(eventPublisher.events).extracting(event -> event.topic())
            .containsExactly("checkin-topic");
        assertThat(eventPublisher.events).extracting(event -> event.eventType())
            .containsExactly("CHECKIN_REQUESTED");
    }

    @Test
    void generatesQrCodeAndValidatesItWhenCheckin() {
        InMemoryCheckinRecordDAO checkinRecordDAO = new InMemoryCheckinRecordDAO();
        LocationService service = new LocationServiceImpl(new InMemoryLocationRecordDAO(),
            new InMemoryUserRealtimeStatusDAO(), checkinRecordDAO, new RecordingDomainEventPublisher(),
            new InMemoryGeoLocationManager());
        CheckinDTO checkinDTO = createCheckinDTO();

        CheckinQrCodeDTO qrCodeDTO = service.generateCheckinQrCode(checkinDTO);
        checkinDTO.setQrCodeToken(qrCodeDTO.getQrCodeToken());
        CheckinDTO savedCheckinDTO = service.checkin(checkinDTO);

        assertThat(qrCodeDTO.getQrCode()).startsWith("data:image/png;base64,");
        assertThat(qrCodeDTO.getQrCodeToken()).startsWith("VSP_CHECKIN:");
        assertThat(qrCodeDTO.getExpireTime()).isNotNull();
        assertThat(savedCheckinDTO.getQrCode()).isEqualTo(qrCodeDTO.getQrCodeToken());
    }

    @Test
    void checkinCanResolveTargetFromQrCodeTokenOnly() {
        LocationService service = new LocationServiceImpl(new InMemoryLocationRecordDAO(),
            new InMemoryUserRealtimeStatusDAO(), new InMemoryCheckinRecordDAO(), new RecordingDomainEventPublisher(),
            new InMemoryGeoLocationManager());
        CheckinQrCodeDTO qrCodeDTO = service.generateCheckinQrCode(createCheckinDTO());
        CheckinDTO checkinDTO = new CheckinDTO();
        checkinDTO.setUserId(101L);
        checkinDTO.setQrCodeToken(qrCodeDTO.getQrCodeToken());
        checkinDTO.setLongitude(new BigDecimal("113.0000000"));
        checkinDTO.setLatitude(new BigDecimal("23.0000000"));

        CheckinDTO savedCheckinDTO = service.checkin(checkinDTO);

        assertThat(savedCheckinDTO.getAssignmentId()).isEqualTo(1L);
        assertThat(savedCheckinDTO.getActivityId()).isEqualTo(100L);
        assertThat(savedCheckinDTO.getPositionId()).isEqualTo(200L);
        assertThat(savedCheckinDTO.getCheckinType()).isEqualTo("CHECK_IN");
    }

    @Test
    void checkinWithPositionQrCodeResolvesUserAssignment() {
        StubScheduleClient scheduleClient = new StubScheduleClient();
        scheduleClient.assignments = List.of(createScheduleAssignment(9L, 100L, 200L, 101L),
            createScheduleAssignment(10L, 100L, 201L, 101L));
        LocationService service = new LocationServiceImpl(new InMemoryLocationRecordDAO(),
            new InMemoryUserRealtimeStatusDAO(), new InMemoryCheckinRecordDAO(), new RecordingDomainEventPublisher(),
            new InMemoryGeoLocationManager(), scheduleClient);
        CheckinDTO qrTargetDTO = new CheckinDTO();
        qrTargetDTO.setActivityId(100L);
        qrTargetDTO.setPositionId(200L);
        qrTargetDTO.setCheckinType("CHECK_IN");
        CheckinQrCodeDTO qrCodeDTO = service.generateCheckinQrCode(qrTargetDTO);
        CheckinDTO checkinDTO = new CheckinDTO();
        checkinDTO.setUserId(101L);
        checkinDTO.setQrCodeToken(qrCodeDTO.getQrCodeToken());
        checkinDTO.setLongitude(new BigDecimal("113.0000000"));
        checkinDTO.setLatitude(new BigDecimal("23.0000000"));

        CheckinDTO savedCheckinDTO = service.checkin(checkinDTO);

        assertThat(savedCheckinDTO.getAssignmentId()).isEqualTo(9L);
        assertThat(savedCheckinDTO.getActivityId()).isEqualTo(100L);
        assertThat(savedCheckinDTO.getPositionId()).isEqualTo(200L);
    }

    @Test
    void listCheckinsCanQueryWholeActivityWithoutUserFilter() {
        LocationService service = new LocationServiceImpl(new InMemoryLocationRecordDAO(),
            new InMemoryUserRealtimeStatusDAO(), new InMemoryCheckinRecordDAO(), new RecordingDomainEventPublisher(),
            new InMemoryGeoLocationManager());
        service.checkin(createCheckinDTO());
        CheckinDTO secondCheckinDTO = createCheckinDTO();
        secondCheckinDTO.setAssignmentId(2L);
        secondCheckinDTO.setUserId(102L);
        service.checkin(secondCheckinDTO);

        assertThat(service.listCheckins(100L, null)).extracting(CheckinDTO::getUserId)
            .containsExactly(101L, 102L);
    }

    @Test
    void checkinThrowsBusinessExceptionWhenQrCodeTargetMismatch() {
        LocationService service = new LocationServiceImpl(new InMemoryLocationRecordDAO(),
            new InMemoryUserRealtimeStatusDAO(), new InMemoryCheckinRecordDAO(), new RecordingDomainEventPublisher(),
            new InMemoryGeoLocationManager());
        CheckinQrCodeDTO qrCodeDTO = service.generateCheckinQrCode(createCheckinDTO());
        CheckinDTO checkinDTO = createCheckinDTO();
        checkinDTO.setPositionId(201L);
        checkinDTO.setQrCodeToken(qrCodeDTO.getQrCodeToken());

        assertThatThrownBy(() -> service.checkin(checkinDTO))
            .isInstanceOf(BusinessException.class)
            .hasMessage("checkin qr code target mismatch");
    }

    @Test
    void checkinThrowsBusinessExceptionWhenDuplicate() {
        LocationService service = new LocationServiceImpl(new InMemoryLocationRecordDAO(),
            new InMemoryUserRealtimeStatusDAO(), new InMemoryCheckinRecordDAO(), new RecordingDomainEventPublisher(),
            new InMemoryGeoLocationManager());
        CheckinDTO checkinDTO = createCheckinDTO();
        service.checkin(checkinDTO);

        assertThatThrownBy(() -> service.checkin(createCheckinDTO()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("checkin already exists");
    }

    @Test
    void getStatusThrowsBusinessExceptionWhenMissing() {
        LocationService service = new LocationServiceImpl(new InMemoryLocationRecordDAO(),
            new InMemoryUserRealtimeStatusDAO(), new InMemoryCheckinRecordDAO(), new RecordingDomainEventPublisher(),
            new InMemoryGeoLocationManager());

        assertThatThrownBy(() -> service.getStatus(100L, 404L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("user realtime status not found");
    }

    @Test
    void reportThrowsBusinessExceptionWhenLocationMissing() {
        LocationService service = new LocationServiceImpl(new InMemoryLocationRecordDAO(),
            new InMemoryUserRealtimeStatusDAO(), new InMemoryCheckinRecordDAO(), new RecordingDomainEventPublisher(),
            new InMemoryGeoLocationManager());

        assertThatThrownBy(() -> service.report(new LocationDTO()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("location report is required");
    }

    private LocationDTO createLocationDTO(Long userId, String status) {
        LocationDTO dto = new LocationDTO();
        dto.setActivityId(100L);
        dto.setUserId(userId);
        dto.setLongitude(new BigDecimal("113.0000000"));
        dto.setLatitude(new BigDecimal("23.0000000"));
        dto.setLocationTime(LocalDateTime.of(2026, 8, 1, 8, 0));
        dto.setStatus(status);
        return dto;
    }

    private CheckinDTO createCheckinDTO() {
        CheckinDTO dto = new CheckinDTO();
        dto.setAssignmentId(1L);
        dto.setActivityId(100L);
        dto.setPositionId(200L);
        dto.setUserId(101L);
        dto.setCheckinType("CHECK_IN");
        dto.setLongitude(new BigDecimal("113.0000000"));
        dto.setLatitude(new BigDecimal("23.0000000"));
        return dto;
    }

    private ScheduleAssignmentDTO createScheduleAssignment(Long id, Long activityId, Long positionId, Long userId) {
        ScheduleAssignmentDTO dto = new ScheduleAssignmentDTO();
        dto.setId(id);
        dto.setActivityId(activityId);
        dto.setPositionId(positionId);
        dto.setUserId(userId);
        return dto;
    }

    private static class InMemoryLocationRecordDAO implements LocationRecordDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);

        @Override
        public int insert(LocationRecordDO locationRecordDO) {
            locationRecordDO.setId(idGenerator.getAndIncrement());
            return 1;
        }
    }

    private static class InMemoryUserRealtimeStatusDAO implements UserRealtimeStatusDAO {

        private final List<UserRealtimeStatusDO> statuses = new ArrayList<>();

        @Override
        public int upsert(UserRealtimeStatusDO statusDO) {
            statuses.removeIf(status -> status.getActivityId().equals(statusDO.getActivityId())
                && status.getUserId().equals(statusDO.getUserId()));
            statuses.add(statusDO);
            return 1;
        }

        @Override
        public UserRealtimeStatusDO selectByUserId(Long activityId, Long userId) {
            return statuses.stream()
                .filter(status -> activityId == null || status.getActivityId().equals(activityId))
                .filter(status -> status.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
        }

        @Override
        public List<UserRealtimeStatusDO> selectNearby(Long activityId, BigDecimal longitude, BigDecimal latitude,
                                                       Integer radiusMeter) {
            return statuses.stream()
                .filter(status -> status.getActivityId().equals(activityId))
                .filter(status -> !"OFFLINE".equals(status.getStatus()))
                .peek(status -> status.setDistanceMeter(distance(status, longitude, latitude)))
                .filter(status -> status.getDistanceMeter().compareTo(BigDecimal.valueOf(radiusMeter)) <= 0)
                .toList();
        }

        private BigDecimal distance(UserRealtimeStatusDO status, BigDecimal longitude, BigDecimal latitude) {
            BigDecimal longitudeDiff = status.getLongitude().subtract(longitude).abs();
            BigDecimal latitudeDiff = status.getLatitude().subtract(latitude).abs();
            return longitudeDiff.add(latitudeDiff).multiply(BigDecimal.valueOf(100000));
        }
    }

    private static class InMemoryCheckinRecordDAO implements CheckinRecordDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<CheckinRecordDO> checkins = new ArrayList<>();

        @Override
        public int insert(CheckinRecordDO checkinRecordDO) {
            checkinRecordDO.setId(idGenerator.getAndIncrement());
            checkins.add(checkinRecordDO);
            return 1;
        }

        @Override
        public int countByAssignmentUserType(Long assignmentId, Long userId, String checkinType) {
            return (int) checkins.stream()
                .filter(checkin -> checkin.getAssignmentId().equals(assignmentId))
                .filter(checkin -> checkin.getUserId().equals(userId))
                .filter(checkin -> checkin.getCheckinType().equals(checkinType))
                .count();
        }

        @Override
        public List<CheckinRecordDO> selectByActivityAndUser(Long activityId, Long userId) {
            return checkins.stream()
                .filter(checkin -> checkin.getActivityId().equals(activityId))
                .filter(checkin -> userId == null || checkin.getUserId().equals(userId))
                .toList();
        }
    }

    private static class RecordingDomainEventPublisher implements DomainEventPublisher {

        private final List<Event> events = new ArrayList<>();

        @Override
        public void publish(String topic, String eventType, String eventKey, Object payload) {
            events.add(new Event(topic, eventType, eventKey, payload));
        }
    }

    private record Event(String topic, String eventType, String eventKey, Object payload) {
    }

    private static class InMemoryGeoLocationManager implements GeoLocationManager {

        private final List<Long> savedUserIds = new ArrayList<>();
        private List<Long> nearbyUserIds = List.of();

        @Override
        public void save(Long activityId, Long userId, BigDecimal longitude, BigDecimal latitude) {
            savedUserIds.add(userId);
        }

        @Override
        public List<Long> nearby(Long activityId, BigDecimal longitude, BigDecimal latitude, Integer radiusMeter) {
            return nearbyUserIds;
        }
    }

    private static class StubScheduleClient implements ScheduleClient {

        private List<ScheduleAssignmentDTO> assignments = List.of();

        @Override
        public Result<com.volunteer.platform.schedule.client.dto.ScheduleDTO> getActivityDetail(Long activityId) {
            return Result.success();
        }

        @Override
        public Result<List<ScheduleAssignmentDTO>> listUserAssignments(Long userId) {
            return Result.success(assignments.stream()
                .filter(assignment -> assignment.getUserId().equals(userId))
                .toList());
        }

        @Override
        public Result<ScheduleDTO> supplementAssignment(SupplementScheduleAssignmentDTO dto) {
            return Result.success();
        }
    }
}
