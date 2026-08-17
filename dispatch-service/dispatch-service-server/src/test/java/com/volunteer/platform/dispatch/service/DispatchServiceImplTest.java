package com.volunteer.platform.dispatch.service;

import com.volunteer.platform.activity.client.api.ActivityClient;
import com.volunteer.platform.activity.client.dto.ActivitySignupDTO;
import com.volunteer.platform.activity.client.dto.AreaDTO;
import com.volunteer.platform.activity.client.dto.PositionDTO;
import com.volunteer.platform.ai.client.api.AiSchedulerClient;
import com.volunteer.platform.ai.client.dto.AiPredictionRequestDTO;
import com.volunteer.platform.ai.client.dto.AiPredictionResultDTO;
import com.volunteer.platform.ai.client.dto.AiRecommendationDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleRequestDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleResultDTO;
import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.mq.DomainEventPublisher;
import com.volunteer.platform.location.client.api.LocationClient;
import com.volunteer.platform.location.client.dto.CheckinDTO;
import com.volunteer.platform.location.client.dto.CheckinQrCodeDTO;
import com.volunteer.platform.location.client.dto.LocationDTO;
import com.volunteer.platform.location.client.dto.NearbyUserDTO;
import com.volunteer.platform.location.client.dto.UserRealtimeStatusDTO;
import com.volunteer.platform.schedule.client.api.ScheduleClient;
import com.volunteer.platform.schedule.client.dto.ScheduleAssignmentDTO;
import com.volunteer.platform.schedule.client.dto.ScheduleDTO;
import com.volunteer.platform.schedule.client.dto.SupplementScheduleAssignmentDTO;
import com.volunteer.platform.dispatch.dao.DispatchRecommendationDAO;
import com.volunteer.platform.dispatch.dao.DispatchTaskDAO;
import com.volunteer.platform.dispatch.dto.DetectShortageDTO;
import com.volunteer.platform.dispatch.dto.ExecuteDispatchDTO;
import com.volunteer.platform.dispatch.entity.DispatchRecommendationDO;
import com.volunteer.platform.dispatch.entity.DispatchTaskDO;
import com.volunteer.platform.dispatch.service.impl.DispatchServiceImpl;
import com.volunteer.platform.dispatch.vo.DispatchResultVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DispatchServiceImplTest {

    @Test
    void executesDispatchTaskAndReturnsRecommendations() {
        RecordingDomainEventPublisher eventPublisher = new RecordingDomainEventPublisher();
        DispatchService service = new DispatchServiceImpl(new InMemoryDispatchTaskDAO(),
            new InMemoryDispatchRecommendationDAO(), eventPublisher, new StubLocationClient(), new StubAiSchedulerClient());

        DispatchResultVO resultVO = service.execute(createDTO(3, List.of(101L, 102L, 103L, 104L)));

        assertThat(resultVO.getId()).isEqualTo(1L);
        assertThat(resultVO.getDispatchStatus()).isEqualTo("FINISHED");
        assertThat(resultVO.getRecommendations()).hasSize(3);
        assertThat(resultVO.getRecommendations()).extracting(recommendation -> recommendation.getUserId())
            .containsExactly(101L, 102L, 103L);
        assertThat(resultVO.getRecommendations()).extracting(recommendation -> recommendation.getRecommendStatus())
            .containsOnly("RECOMMENDED");
        assertThat(eventPublisher.events).extracting(event -> event.topic())
            .containsExactly("message-topic", "message-topic", "message-topic", "dispatch-topic");
        assertThat(eventPublisher.events).extracting(event -> event.eventType())
            .containsExactly("DISPATCH_NOTICE_REQUESTED", "DISPATCH_NOTICE_REQUESTED",
                "DISPATCH_NOTICE_REQUESTED", "DISPATCH_FINISHED");
        assertThat(eventPublisher.events.subList(0, 3))
            .extracting(event -> Long.valueOf(((Map<?, ?>) event.payload()).get("receiverId").toString()))
            .containsExactly(101L, 102L, 103L);
    }

    @Test
    void executeMarksPartialWhenCandidatesAreNotEnough() {
        DispatchService service = new DispatchServiceImpl(new InMemoryDispatchTaskDAO(),
            new InMemoryDispatchRecommendationDAO(), new RecordingDomainEventPublisher(), new StubLocationClient(),
            new StubAiSchedulerClient());

        DispatchResultVO resultVO = service.execute(createDTO(3, List.of(101L)));

        assertThat(resultVO.getDispatchStatus()).isEqualTo("PARTIAL");
        assertThat(resultVO.getRecommendations()).hasSize(1);
    }

    @Test
    void executeMarksNoCandidateWhenCandidateListIsEmpty() {
        DispatchService service = new DispatchServiceImpl(new InMemoryDispatchTaskDAO(),
            new InMemoryDispatchRecommendationDAO(), new RecordingDomainEventPublisher(), new StubLocationClient(),
            new StubAiSchedulerClient());

        DispatchResultVO resultVO = service.execute(createDTO(2, List.of()));

        assertThat(resultVO.getDispatchStatus()).isEqualTo("NO_CANDIDATE");
        assertThat(resultVO.getRecommendations()).isEmpty();
    }

    @Test
    void executeUsesNearbyUsersFromLocationServiceWhenCandidatesAreEmpty() {
        StubLocationClient locationClient = new StubLocationClient();
        locationClient.nearbyUsers = List.of(createNearbyUser(201L), createNearbyUser(202L), createNearbyUser(203L));
        DispatchService service = new DispatchServiceImpl(new InMemoryDispatchTaskDAO(),
            new InMemoryDispatchRecommendationDAO(), new RecordingDomainEventPublisher(), locationClient,
            new StubAiSchedulerClient());
        ExecuteDispatchDTO dto = createDTO(2, List.of());
        dto.setLongitude(new BigDecimal("113.0000000"));
        dto.setLatitude(new BigDecimal("23.0000000"));
        dto.setRadiusMeter(500);

        DispatchResultVO resultVO = service.execute(dto);

        assertThat(locationClient.nearbyCalled).isTrue();
        assertThat(resultVO.getDispatchStatus()).isEqualTo("FINISHED");
        assertThat(resultVO.getRecommendations()).extracting(recommendation -> recommendation.getUserId())
            .containsExactly(201L, 202L);
    }

    @Test
    void executeUsesApprovedUnscheduledSignupsWhenCandidatesAreEmpty() {
        StubScheduleClient scheduleClient = new StubScheduleClient();
        ScheduleDTO scheduleDTO = createScheduleDTO();
        scheduleDTO.setAssignments(List.of(createScheduleAssignment(1001L, 200L, 300L, 101L),
            createScheduleAssignment(1002L, 200L, 300L, 102L)));
        scheduleClient.scheduleDTO = scheduleDTO;
        StubActivityClient activityClient = new StubActivityClient();
        activityClient.signups = List.of(createSignup(100L, 300L, 101L), createSignup(100L, 300L, 102L),
            createSignup(100L, 300L, 103L), createSignup(100L, 301L, 104L));
        DispatchService service = new DispatchServiceImpl(new InMemoryDispatchTaskDAO(),
            new InMemoryDispatchRecommendationDAO(), new RecordingDomainEventPublisher(), new StubLocationClient(),
            new StubAiSchedulerClient(), scheduleClient, activityClient);

        DispatchResultVO resultVO = service.execute(createDTO(1, List.of()));

        assertThat(resultVO.getDispatchStatus()).isEqualTo("FINISHED");
        assertThat(resultVO.getRecommendations()).extracting(recommendation -> recommendation.getUserId())
            .containsExactly(103L);
    }

    @Test
    void executeUsesAiSchedulerRankingWhenCandidatesAreAvailable() {
        StubAiSchedulerClient aiSchedulerClient = new StubAiSchedulerClient();
        aiSchedulerClient.recommendations = List.of(createAiRecommendation(103L, 1, "98.50"),
            createAiRecommendation(101L, 2, "95.00"));
        DispatchService service = new DispatchServiceImpl(new InMemoryDispatchTaskDAO(),
            new InMemoryDispatchRecommendationDAO(), new RecordingDomainEventPublisher(), new StubLocationClient(),
            aiSchedulerClient);

        DispatchResultVO resultVO = service.execute(createDTO(2, List.of(101L, 102L, 103L)));

        assertThat(aiSchedulerClient.recommendCalled).isTrue();
        assertThat(aiSchedulerClient.requestDTO.getActivityId()).isEqualTo(100L);
        assertThat(aiSchedulerClient.requestDTO.getPositionId()).isEqualTo(300L);
        assertThat(resultVO.getRecommendations()).extracting(recommendation -> recommendation.getUserId())
            .containsExactly(103L, 101L);
        assertThat(resultVO.getRecommendations()).extracting(recommendation -> recommendation.getMatchScore())
            .containsExactly(new BigDecimal("98.50"), new BigDecimal("95.00"));
    }

    @Test
    void executeSkipsCandidatesWithOverlappingScheduleTime() {
        StubScheduleClient scheduleClient = new StubScheduleClient();
        ScheduleAssignmentDTO targetAssignment = createScheduleAssignment(1001L, 200L, 300L, 101L);
        ScheduleAssignmentDTO conflictingAssignment = createScheduleAssignment(2001L, 201L, 301L, 201L);
        scheduleClient.scheduleDTO = createScheduleDTO();
        scheduleClient.scheduleDTO.setAssignments(List.of(targetAssignment, conflictingAssignment));
        DispatchService service = new DispatchServiceImpl(new InMemoryDispatchTaskDAO(),
            new InMemoryDispatchRecommendationDAO(), new RecordingDomainEventPublisher(), new StubLocationClient(),
            new StubAiSchedulerClient(), scheduleClient, new StubActivityClient());

        DispatchResultVO resultVO = service.execute(createDTO(1, List.of(201L, 202L)));

        assertThat(resultVO.getDispatchStatus()).isEqualTo("FINISHED");
        assertThat(resultVO.getRecommendations()).extracting(recommendation -> recommendation.getUserId())
            .containsExactly(202L);
    }

    @Test
    void detectsStaffShortageAndTriggersDispatchForMissingCount() {
        StubScheduleClient scheduleClient = new StubScheduleClient();
        scheduleClient.scheduleDTO = createScheduleDTO();
        StubLocationClient locationClient = new StubLocationClient();
        locationClient.checkins = List.of(createCheckin(1001L, 101L));
        locationClient.nearbyUsers = List.of(createNearbyUser(101L), createNearbyUser(201L), createNearbyUser(202L));
        DispatchService service = new DispatchServiceImpl(new InMemoryDispatchTaskDAO(),
            new InMemoryDispatchRecommendationDAO(), new RecordingDomainEventPublisher(), locationClient,
            new StubAiSchedulerClient(), scheduleClient);

        List<DispatchResultVO> resultVOS = service.detectShortage(createDetectShortageDTO());

        assertThat(resultVOS).hasSize(1);
        assertThat(resultVOS.get(0).getRequiredCount()).isEqualTo(2);
        assertThat(resultVOS.get(0).getReason()).isEqualTo("STAFF_SHORTAGE");
        assertThat(resultVOS.get(0).getRecommendations()).extracting(recommendation -> recommendation.getUserId())
            .containsExactly(201L, 202L);
        assertThat(locationClient.listCheckinsCalled).isEqualTo(3);
        assertThat(locationClient.nearbyCalled).isTrue();
    }

    @Test
    void detectsStaffShortageAndPrefersApprovedUnscheduledSignupCandidates() {
        StubScheduleClient scheduleClient = new StubScheduleClient();
        ScheduleDTO scheduleDTO = createScheduleDTO();
        scheduleDTO.setAssignments(List.of(createScheduleAssignment(1001L, 200L, 300L, 101L),
            createScheduleAssignment(1002L, 200L, 300L, 102L)));
        scheduleClient.scheduleDTO = scheduleDTO;
        StubLocationClient locationClient = new StubLocationClient();
        locationClient.checkins = List.of(createCheckin(1001L, 101L));
        locationClient.nearbyUsers = List.of(createNearbyUser(201L));
        StubActivityClient activityClient = new StubActivityClient();
        activityClient.signups = List.of(createSignup(100L, 300L, 101L), createSignup(100L, 300L, 102L),
            createSignup(100L, 300L, 103L), createSignup(100L, 301L, 104L));
        DispatchService service = new DispatchServiceImpl(new InMemoryDispatchTaskDAO(),
            new InMemoryDispatchRecommendationDAO(), new RecordingDomainEventPublisher(), locationClient,
            new StubAiSchedulerClient(), scheduleClient, activityClient);

        List<DispatchResultVO> resultVOS = service.detectShortage(createDetectShortageDTO());

        assertThat(resultVOS).hasSize(1);
        assertThat(resultVOS.get(0).getRequiredCount()).isEqualTo(1);
        assertThat(resultVOS.get(0).getRecommendations()).extracting(recommendation -> recommendation.getUserId())
            .containsExactly(103L);
        assertThat(locationClient.nearbyCalled).isTrue();
    }

    @Test
    void detectsNoShortageWhenAllAssignmentsCheckedIn() {
        StubScheduleClient scheduleClient = new StubScheduleClient();
        scheduleClient.scheduleDTO = createScheduleDTO();
        StubLocationClient locationClient = new StubLocationClient();
        locationClient.checkins = List.of(createCheckin(1001L, 101L), createCheckin(1002L, 102L),
            createCheckin(1003L, 103L));
        DispatchService service = new DispatchServiceImpl(new InMemoryDispatchTaskDAO(),
            new InMemoryDispatchRecommendationDAO(), new RecordingDomainEventPublisher(), locationClient,
            new StubAiSchedulerClient(), scheduleClient);

        List<DispatchResultVO> resultVOS = service.detectShortage(createDetectShortageDTO());

        assertThat(resultVOS).isEmpty();
    }

    @Test
    void acceptsRecommendationAndAddsSupplementAssignment() {
        InMemoryDispatchRecommendationDAO recommendationDAO = new InMemoryDispatchRecommendationDAO();
        StubScheduleClient scheduleClient = new StubScheduleClient();
        scheduleClient.scheduleDTO = createScheduleDTO();
        DispatchService service = new DispatchServiceImpl(new InMemoryDispatchTaskDAO(), recommendationDAO,
            new RecordingDomainEventPublisher(), new StubLocationClient(), new StubAiSchedulerClient(), scheduleClient,
            new StubActivityClient());
        DispatchResultVO task = service.execute(createDTO(1, List.of(201L)));

        DispatchResultVO accepted = service.acceptRecommendation(task.getRecommendations().get(0).getId());

        assertThat(scheduleClient.supplementCalled).isTrue();
        assertThat(scheduleClient.supplementDTO.getActivityId()).isEqualTo(100L);
        assertThat(scheduleClient.supplementDTO.getAreaId()).isEqualTo(200L);
        assertThat(scheduleClient.supplementDTO.getPositionId()).isEqualTo(300L);
        assertThat(scheduleClient.supplementDTO.getUserId()).isEqualTo(201L);
        assertThat(accepted.getRecommendations()).extracting(recommendation -> recommendation.getRecommendStatus())
            .containsExactly("ACCEPTED");
    }

    @Test
    void getResultThrowsBusinessExceptionWhenTaskMissing() {
        DispatchService service = new DispatchServiceImpl(new InMemoryDispatchTaskDAO(),
            new InMemoryDispatchRecommendationDAO(), new RecordingDomainEventPublisher(), new StubLocationClient(),
            new StubAiSchedulerClient());

        assertThatThrownBy(() -> service.getResult(404L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("dispatch task not found");
    }

    @Test
    void executeThrowsBusinessExceptionWhenRequiredCountInvalid() {
        DispatchService service = new DispatchServiceImpl(new InMemoryDispatchTaskDAO(),
            new InMemoryDispatchRecommendationDAO(), new RecordingDomainEventPublisher(), new StubLocationClient(),
            new StubAiSchedulerClient());
        ExecuteDispatchDTO dto = createDTO(0, List.of(101L));

        assertThatThrownBy(() -> service.execute(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("required count must be positive");
    }

    private ExecuteDispatchDTO createDTO(Integer requiredCount, List<Long> candidateUserIds) {
        ExecuteDispatchDTO dto = new ExecuteDispatchDTO();
        dto.setActivityId(100L);
        dto.setAreaId(200L);
        dto.setPositionId(300L);
        dto.setRequiredCount(requiredCount);
        dto.setReason("LACK_OF_STAFF");
        dto.setCreatedBy(1L);
        dto.setCandidateUserIds(candidateUserIds);
        return dto;
    }

    private DetectShortageDTO createDetectShortageDTO() {
        DetectShortageDTO dto = new DetectShortageDTO();
        dto.setActivityId(100L);
        dto.setLongitude(new BigDecimal("113.0000000"));
        dto.setLatitude(new BigDecimal("23.0000000"));
        dto.setRadiusMeter(500);
        dto.setCreatedBy(1L);
        return dto;
    }

    private ScheduleDTO createScheduleDTO() {
        ScheduleDTO dto = new ScheduleDTO();
        dto.setActivityId(100L);
        dto.setPlanId(1L);
        dto.setPlanStatus("PUBLISHED");
        dto.setAssignments(List.of(createScheduleAssignment(1001L, 200L, 300L, 101L),
            createScheduleAssignment(1002L, 200L, 300L, 102L),
            createScheduleAssignment(1003L, 200L, 300L, 103L)));
        return dto;
    }

    private ScheduleAssignmentDTO createScheduleAssignment(Long id, Long areaId, Long positionId, Long userId) {
        ScheduleAssignmentDTO dto = new ScheduleAssignmentDTO();
        dto.setId(id);
        dto.setAreaId(areaId);
        dto.setPositionId(positionId);
        dto.setUserId(userId);
        dto.setWorkDate(LocalDate.of(2026, 8, 1));
        dto.setStartTime(LocalDateTime.of(2026, 8, 1, 9, 0));
        dto.setEndTime(LocalDateTime.of(2026, 8, 1, 12, 0));
        dto.setAssignmentStatus("CONFIRMED");
        return dto;
    }

    private CheckinDTO createCheckin(Long assignmentId, Long userId) {
        CheckinDTO dto = new CheckinDTO();
        dto.setAssignmentId(assignmentId);
        dto.setActivityId(100L);
        dto.setPositionId(300L);
        dto.setUserId(userId);
        dto.setCheckinStatus("NORMAL");
        dto.setCheckinTime(LocalDateTime.of(2026, 8, 1, 8, 55));
        return dto;
    }

    private ActivitySignupDTO createSignup(Long activityId, Long positionId, Long userId) {
        ActivitySignupDTO dto = new ActivitySignupDTO();
        dto.setActivityId(activityId);
        dto.setPositionId(positionId);
        dto.setUserId(userId);
        dto.setSignupStatus("APPROVED");
        return dto;
    }

    private NearbyUserDTO createNearbyUser(Long userId) {
        NearbyUserDTO dto = new NearbyUserDTO();
        dto.setUserId(userId);
        dto.setLongitude(new BigDecimal("113.0000000"));
        dto.setLatitude(new BigDecimal("23.0000000"));
        dto.setDistanceMeter(BigDecimal.ZERO);
        dto.setStatus("ONLINE");
        return dto;
    }

    private AiRecommendationDTO createAiRecommendation(Long userId, Integer rankNo, String matchScore) {
        AiRecommendationDTO dto = new AiRecommendationDTO();
        dto.setUserId(userId);
        dto.setRankNo(rankNo);
        dto.setMatchScore(new BigDecimal(matchScore));
        dto.setReason("ai");
        return dto;
    }

    private static class InMemoryDispatchTaskDAO implements DispatchTaskDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<DispatchTaskDO> tasks = new ArrayList<>();

        @Override
        public int insert(DispatchTaskDO dispatchTaskDO) {
            dispatchTaskDO.setId(idGenerator.getAndIncrement());
            tasks.add(dispatchTaskDO);
            return 1;
        }

        @Override
        public DispatchTaskDO selectById(Long id) {
            return tasks.stream()
                .filter(task -> task.getId().equals(id))
                .findFirst()
                .orElse(null);
        }

        @Override
        public int updateStatus(DispatchTaskDO dispatchTaskDO) {
            DispatchTaskDO oldTask = selectById(dispatchTaskDO.getId());
            if (oldTask == null) {
                return 0;
            }
            oldTask.setDispatchStatus(dispatchTaskDO.getDispatchStatus());
            oldTask.setFinishedTime(dispatchTaskDO.getFinishedTime());
            return 1;
        }
    }

    private static class InMemoryDispatchRecommendationDAO implements DispatchRecommendationDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<DispatchRecommendationDO> recommendations = new ArrayList<>();

        @Override
        public int insert(DispatchRecommendationDO recommendationDO) {
            recommendationDO.setId(idGenerator.getAndIncrement());
            recommendations.add(recommendationDO);
            return 1;
        }

        @Override
        public List<DispatchRecommendationDO> selectByTaskId(Long dispatchTaskId) {
            return recommendations.stream()
                .filter(recommendation -> recommendation.getDispatchTaskId().equals(dispatchTaskId))
                .toList();
        }

        @Override
        public DispatchRecommendationDO selectById(Long id) {
            return recommendations.stream()
                .filter(recommendation -> recommendation.getId().equals(id))
                .findFirst()
                .orElse(null);
        }

        @Override
        public int updateStatus(DispatchRecommendationDO recommendationDO) {
            DispatchRecommendationDO oldRecommendation = selectById(recommendationDO.getId());
            if (oldRecommendation == null) {
                return 0;
            }
            oldRecommendation.setRecommendStatus(recommendationDO.getRecommendStatus());
            return 1;
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

    private static class StubLocationClient implements LocationClient {

        private boolean nearbyCalled;
        private int listCheckinsCalled;
        private List<NearbyUserDTO> nearbyUsers = List.of();
        private List<CheckinDTO> checkins = List.of();

        @Override
        public Result<UserRealtimeStatusDTO> report(LocationDTO locationDTO) {
            return Result.success();
        }

        @Override
        public Result<List<NearbyUserDTO>> nearby(Long activityId, BigDecimal longitude, BigDecimal latitude,
                                                  Integer radiusMeter) {
            nearbyCalled = true;
            return Result.success(nearbyUsers);
        }

        @Override
        public Result<UserRealtimeStatusDTO> getStatus(Long userId, Long activityId) {
            return Result.success();
        }

        @Override
        public Result<CheckinDTO> checkin(CheckinDTO checkinDTO) {
            return Result.success();
        }

        @Override
        public Result<CheckinDTO> submitAsyncCheckin(CheckinDTO checkinDTO) {
            return Result.success(checkinDTO);
        }

        @Override
        public Result<CheckinQrCodeDTO> generateCheckinQrCode(CheckinDTO checkinDTO) {
            CheckinQrCodeDTO qrCodeDTO = new CheckinQrCodeDTO();
            qrCodeDTO.setActivityId(checkinDTO.getActivityId());
            qrCodeDTO.setAssignmentId(checkinDTO.getAssignmentId());
            qrCodeDTO.setQrCode("test-qrcode");
            return Result.success(qrCodeDTO);
        }

        @Override
        public Result<List<CheckinDTO>> listCheckins(Long activityId, Long userId) {
            listCheckinsCalled++;
            return Result.success(checkins.stream()
                .filter(checkin -> checkin.getActivityId().equals(activityId))
                .filter(checkin -> checkin.getUserId().equals(userId))
                .toList());
        }
    }

    private static class StubScheduleClient implements ScheduleClient {

        private ScheduleDTO scheduleDTO;
        private boolean supplementCalled;
        private SupplementScheduleAssignmentDTO supplementDTO;

        @Override
        public Result<ScheduleDTO> getActivityDetail(Long activityId) {
            return Result.success(scheduleDTO);
        }

        @Override
        public Result<List<ScheduleAssignmentDTO>> listUserAssignments(Long userId) {
            return Result.success(scheduleDTO == null ? List.of() : scheduleDTO.getAssignments().stream()
                .filter(assignment -> assignment.getUserId().equals(userId))
                .toList());
        }

        @Override
        public Result<ScheduleDTO> supplementAssignment(SupplementScheduleAssignmentDTO dto) {
            supplementCalled = true;
            supplementDTO = dto;
            return Result.success(scheduleDTO);
        }
    }

    private static class StubActivityClient implements ActivityClient {

        private List<ActivitySignupDTO> signups = List.of();

        @Override
        public Result<List<PositionDTO>> getPositionList(Long activityId) {
            return Result.success(List.of());
        }

        @Override
        public Result<PositionDTO> getPosition(Long id) {
            return Result.success();
        }

        @Override
        public Result<List<AreaDTO>> getAreaList(Long activityId) {
            return Result.success(List.of());
        }

        @Override
        public Result<List<ActivitySignupDTO>> listSignups(Long activityId, String signupStatus) {
            return Result.success(signups.stream()
                .filter(signup -> signup.getActivityId().equals(activityId))
                .filter(signup -> signupStatus.equals(signup.getSignupStatus()))
                .toList());
        }
    }

    private static class StubAiSchedulerClient implements AiSchedulerClient {

        private boolean recommendCalled;
        private AiScheduleRequestDTO requestDTO;
        private List<AiRecommendationDTO> recommendations = List.of();

        @Override
        public Result<AiScheduleResultDTO> recommend(AiScheduleRequestDTO requestDTO) {
            recommendCalled = true;
            this.requestDTO = requestDTO;
            AiScheduleResultDTO resultDTO = new AiScheduleResultDTO();
            resultDTO.setActivityId(requestDTO.getActivityId());
            resultDTO.setPositionId(requestDTO.getPositionId());
            resultDTO.setRecommendations(recommendations);
            return Result.success(resultDTO);
        }

        @Override
        public Result<AiPredictionResultDTO> predictRisks(AiPredictionRequestDTO requestDTO) {
            return Result.success(new AiPredictionResultDTO());
        }
    }

}
