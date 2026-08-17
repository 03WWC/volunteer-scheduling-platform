package com.volunteer.platform.schedule.service;

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
import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.mq.DomainEventPublisher;
import com.volunteer.platform.schedule.dao.ScheduleAssignmentDAO;
import com.volunteer.platform.schedule.dao.SchedulePlanDAO;
import com.volunteer.platform.schedule.dto.AutoGenerateScheduleDTO;
import com.volunteer.platform.schedule.dto.GenerateScheduleDTO;
import com.volunteer.platform.schedule.dto.ScheduleAssignmentDTO;
import com.volunteer.platform.schedule.client.dto.SupplementScheduleAssignmentDTO;
import com.volunteer.platform.schedule.entity.ScheduleAssignmentDO;
import com.volunteer.platform.schedule.entity.SchedulePlanDO;
import com.volunteer.platform.schedule.manager.FreeVolunteerCacheManager;
import com.volunteer.platform.schedule.manager.ScheduleLockManager;
import com.volunteer.platform.schedule.service.impl.ScheduleServiceImpl;
import com.volunteer.platform.schedule.vo.ScheduleAssignmentVO;
import com.volunteer.platform.schedule.vo.ScheduleDetailVO;
import com.volunteer.platform.user.client.api.UserClient;
import com.volunteer.platform.user.client.dto.UserAvailabilityDTO;
import com.volunteer.platform.user.client.dto.UserDTO;
import com.volunteer.platform.user.client.dto.UserSkillDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleServiceImplTest {

    @Test
    void generatesSchedulePlanWithAssignmentsAndQueriesDetail() {
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(),
            new InMemoryScheduleAssignmentDAO(), new RecordingDomainEventPublisher());
        GenerateScheduleDTO dto = new GenerateScheduleDTO();
        dto.setActivityId(100L);
        dto.setPlanName("第一版排班");
        dto.setGeneratedBy(1L);
        dto.setAssignments(List.of(createAssignment(10L, 20L, 30L), createAssignment(11L, 21L, 31L)));

        ScheduleDetailVO generated = service.generate(dto);
        ScheduleDetailVO detail = service.getActivityDetail(100L);

        assertThat(generated.getPlanId()).isEqualTo(1L);
        assertThat(generated.getPlanStatus()).isEqualTo("GENERATED");
        assertThat(generated.getAssignments()).hasSize(2);
        assertThat(generated.getAssignments()).extracting(assignment -> assignment.getAssignmentStatus())
            .containsOnly("WAIT_CONFIRM");
        assertThat(detail.getAssignments()).extracting(assignment -> assignment.getUserId())
            .containsExactly(30L, 31L);
    }

    @Test
    void getActivityDetailThrowsBusinessExceptionWhenPlanMissing() {
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(),
            new InMemoryScheduleAssignmentDAO(), new RecordingDomainEventPublisher());

        assertThatThrownBy(() -> service.getActivityDetail(404L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("schedule plan not found");
    }

    @Test
    void generateThrowsBusinessExceptionWhenSameUserTimeOverlaps() {
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(),
            new InMemoryScheduleAssignmentDAO(), new RecordingDomainEventPublisher());
        GenerateScheduleDTO dto = new GenerateScheduleDTO();
        dto.setActivityId(100L);
        dto.setPlanName("冲突排班");
        dto.setGeneratedBy(1L);
        ScheduleAssignmentDTO firstAssignment = createAssignment(10L, 20L, 30L);
        ScheduleAssignmentDTO secondAssignment = createAssignment(11L, 21L, 30L);
        secondAssignment.setStartTime(LocalDateTime.of(2026, 8, 1, 11, 0));
        secondAssignment.setEndTime(LocalDateTime.of(2026, 8, 1, 13, 0));
        dto.setAssignments(List.of(firstAssignment, secondAssignment));

        assertThatThrownBy(() -> service.generate(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("schedule time conflict");
    }

    @Test
    void generateThrowsBusinessExceptionWhenExistingAssignmentOverlaps() {
        InMemoryScheduleAssignmentDAO assignmentDAO = new InMemoryScheduleAssignmentDAO();
        ScheduleAssignmentDO existingAssignment = new ScheduleAssignmentDO();
        existingAssignment.setPlanId(1L);
        existingAssignment.setActivityId(100L);
        existingAssignment.setAreaId(10L);
        existingAssignment.setPositionId(20L);
        existingAssignment.setUserId(30L);
        existingAssignment.setWorkDate(LocalDate.of(2026, 8, 1));
        existingAssignment.setStartTime(LocalDateTime.of(2026, 8, 1, 9, 0));
        existingAssignment.setEndTime(LocalDateTime.of(2026, 8, 1, 12, 0));
        existingAssignment.setAssignmentStatus("WAIT_CONFIRM");
        assignmentDAO.insert(existingAssignment);
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(), assignmentDAO,
            new RecordingDomainEventPublisher());
        GenerateScheduleDTO dto = new GenerateScheduleDTO();
        dto.setActivityId(100L);
        dto.setPlanName("历史冲突排班");
        dto.setGeneratedBy(1L);
        ScheduleAssignmentDTO assignmentDTO = createAssignment(11L, 21L, 30L);
        assignmentDTO.setStartTime(LocalDateTime.of(2026, 8, 1, 11, 0));
        assignmentDTO.setEndTime(LocalDateTime.of(2026, 8, 1, 13, 0));
        dto.setAssignments(List.of(assignmentDTO));

        assertThatThrownBy(() -> service.generate(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("schedule time conflict");
    }

    @Test
    void publishesPlanAndConfirmsAssignment() {
        InMemorySchedulePlanDAO planDAO = new InMemorySchedulePlanDAO();
        InMemoryScheduleAssignmentDAO assignmentDAO = new InMemoryScheduleAssignmentDAO();
        RecordingDomainEventPublisher eventPublisher = new RecordingDomainEventPublisher();
        ScheduleService service = new ScheduleServiceImpl(planDAO, assignmentDAO, eventPublisher);
        GenerateScheduleDTO dto = new GenerateScheduleDTO();
        dto.setActivityId(100L);
        dto.setPlanName("待发布排班");
        dto.setGeneratedBy(1L);
        dto.setAssignments(List.of(createAssignment(10L, 20L, 30L)));
        ScheduleDetailVO generated = service.generate(dto);

        ScheduleDetailVO published = service.publish(generated.getPlanId());
        service.confirmAssignment(generated.getAssignments().get(0).getId());
        ScheduleDetailVO detail = service.getActivityDetail(100L);

        assertThat(published.getPlanStatus()).isEqualTo("PUBLISHED");
        assertThat(detail.getAssignments()).extracting(assignment -> assignment.getAssignmentStatus())
            .containsExactly("CONFIRMED");
        assertThat(eventPublisher.events).extracting(event -> event.topic())
            .contains("schedule-topic");
        assertThat(eventPublisher.events).extracting(event -> event.eventType())
            .contains("SCHEDULE_PUBLISHED");
    }

    @Test
    void publishDoesNotSendDuplicateEventWhenPlanAlreadyPublished() {
        InMemorySchedulePlanDAO planDAO = new InMemorySchedulePlanDAO();
        RecordingDomainEventPublisher eventPublisher = new RecordingDomainEventPublisher();
        ScheduleService service = new ScheduleServiceImpl(planDAO, new InMemoryScheduleAssignmentDAO(), eventPublisher);
        ScheduleDetailVO generated = service.generate(createGenerateScheduleDTO());

        service.publish(generated.getPlanId());
        ScheduleDetailVO publishedAgain = service.publish(generated.getPlanId());

        assertThat(publishedAgain.getPlanStatus()).isEqualTo("PUBLISHED");
        assertThat(eventPublisher.events).extracting(event -> event.eventType())
            .containsExactly("SCHEDULE_PUBLISHED");
    }

    @Test
    void publishThrowsBusinessExceptionWhenPlanMissing() {
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(),
            new InMemoryScheduleAssignmentDAO(), new RecordingDomainEventPublisher());

        assertThatThrownBy(() -> service.publish(404L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("schedule plan not found");
    }

    @Test
    void confirmAssignmentThrowsBusinessExceptionWhenMissing() {
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(),
            new InMemoryScheduleAssignmentDAO(), new RecordingDomainEventPublisher());

        assertThatThrownBy(() -> service.confirmAssignment(404L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("schedule assignment not found");
    }

    @Test
    void generateThrowsBusinessExceptionWhenScheduleLockIsHeld() {
        DenyingScheduleLockManager lockManager = new DenyingScheduleLockManager();
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(),
            new InMemoryScheduleAssignmentDAO(), new RecordingDomainEventPublisher(), lockManager);

        assertThatThrownBy(() -> service.generate(createGenerateScheduleDTO()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("schedule generation in progress");
        assertThat(lockManager.releasedActivityIds).isEmpty();
    }

    @Test
    void generateReleasesScheduleLockAfterSuccess() {
        AllowingScheduleLockManager lockManager = new AllowingScheduleLockManager();
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(),
            new InMemoryScheduleAssignmentDAO(), new RecordingDomainEventPublisher(), lockManager);

        service.generate(createGenerateScheduleDTO());

        assertThat(lockManager.releasedActivityIds).containsExactly(100L);
    }

    @Test
    void generateReleasesScheduleLockAfterBusinessFailure() {
        AllowingScheduleLockManager lockManager = new AllowingScheduleLockManager();
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(),
            new FailingScheduleAssignmentDAO(), new RecordingDomainEventPublisher(), lockManager);

        assertThatThrownBy(() -> service.generate(createGenerateScheduleDTO()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("schedule time conflict");
        assertThat(lockManager.releasedActivityIds).containsExactly(100L);
    }

    @Test
    void autoGeneratesScheduleByPositionNeedSkillAndTimeConflict() {
        InMemoryScheduleAssignmentDAO assignmentDAO = new InMemoryScheduleAssignmentDAO();
        StubActivityClient activityClient = new StubActivityClient();
        activityClient.positions = List.of(createPosition(20L, 10L, 2, "SECURITY",
                LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 12, 0)),
            createPosition(21L, 10L, 1, "GUIDE",
                LocalDateTime.of(2026, 8, 1, 10, 0), LocalDateTime.of(2026, 8, 1, 12, 0)));
        StubUserClient userClient = new StubUserClient();
        userClient.volunteers = List.of(createUser(101L), createUser(102L), createUser(103L));
        userClient.skills = List.of(createSkill(101L, "SECURITY"), createSkill(102L, "SECURITY"),
            createSkill(102L, "GUIDE"), createSkill(103L, "GUIDE"));
        RecordingFreeVolunteerCacheManager cacheManager = new RecordingFreeVolunteerCacheManager();
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(), assignmentDAO,
            new RecordingDomainEventPublisher(), new AllowingScheduleLockManager(), activityClient, userClient,
            cacheManager);

        ScheduleDetailVO detailVO = service.autoGenerate(createAutoGenerateScheduleDTO());

        assertThat(detailVO.getAssignments()).hasSize(3);
        assertThat(detailVO.getAssignments()).extracting(assignment -> assignment.getPositionId())
            .containsExactly(20L, 20L, 21L);
        assertThat(detailVO.getAssignments()).extracting(assignment -> assignment.getUserId())
            .containsExactly(101L, 102L, 103L);
        assertThat(cacheManager.cachedUserIds).containsExactly(101L, 102L, 103L);
    }

    @Test
    void autoGenerateIgnoresStaleCachedFreeVolunteerIds() {
        StubActivityClient activityClient = new StubActivityClient();
        activityClient.positions = List.of(createPosition(20L, 10L, 1, null,
            LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 12, 0)));
        activityClient.signups = List.of(createSignup(100L, 20L, 101L));
        StubUserClient userClient = new StubUserClient();
        userClient.volunteers = List.of(createUser(101L), createUser(102L));
        RecordingFreeVolunteerCacheManager cacheManager = new RecordingFreeVolunteerCacheManager();
        cacheManager.cachedLookupUserIds = List.of(102L);
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(),
            new InMemoryScheduleAssignmentDAO(), new RecordingDomainEventPublisher(), new AllowingScheduleLockManager(),
            activityClient, userClient, cacheManager);

        ScheduleDetailVO detailVO = service.autoGenerate(createAutoGenerateScheduleDTO());

        assertThat(detailVO.getAssignments()).extracting(assignment -> assignment.getUserId())
            .containsExactly(101L);
        assertThat(userClient.listVolunteersCalled).isTrue();
        assertThat(cacheManager.cachedUserIds).containsExactly(101L);
    }

    @Test
    void autoGenerateAllowsSameVolunteerWhenTimeDoesNotOverlap() {
        StubActivityClient activityClient = new StubActivityClient();
        activityClient.positions = List.of(createPosition(20L, 10L, 1, null,
                LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 12, 0)),
            createPosition(21L, 10L, 1, null,
                LocalDateTime.of(2026, 8, 1, 13, 0), LocalDateTime.of(2026, 8, 1, 16, 0)));
        StubUserClient userClient = new StubUserClient();
        userClient.volunteers = List.of(createUser(101L));
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(),
            new InMemoryScheduleAssignmentDAO(), new RecordingDomainEventPublisher(), new AllowingScheduleLockManager(),
            activityClient, userClient, new RecordingFreeVolunteerCacheManager());

        ScheduleDetailVO detailVO = service.autoGenerate(createAutoGenerateScheduleDTO());

        assertThat(detailVO.getAssignments()).extracting(assignment -> assignment.getPositionId())
            .containsExactly(20L, 21L);
        assertThat(detailVO.getAssignments()).extracting(assignment -> assignment.getUserId())
            .containsExactly(101L, 101L);
    }

    @Test
    void autoGenerateUsesAiRecommendationOrderWhenAvailable() {
        StubActivityClient activityClient = new StubActivityClient();
        activityClient.positions = List.of(createPosition(20L, 10L, 2, "SECURITY",
            LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 12, 0)));
        StubUserClient userClient = new StubUserClient();
        userClient.volunteers = List.of(createUser(101L), createUser(102L), createUser(103L));
        userClient.skills = List.of(createSkill(101L, "SECURITY"), createSkill(102L, "SECURITY"),
            createSkill(103L, "SECURITY"));
        StubAiSchedulerClient aiSchedulerClient = new StubAiSchedulerClient();
        aiSchedulerClient.resultDTO = createAiScheduleResult(20L, List.of(103L, 101L));
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(),
            new InMemoryScheduleAssignmentDAO(), new RecordingDomainEventPublisher(), new AllowingScheduleLockManager(),
            activityClient, userClient, new RecordingFreeVolunteerCacheManager(), aiSchedulerClient);

        ScheduleDetailVO detailVO = service.autoGenerate(createAutoGenerateScheduleDTO());

        assertThat(aiSchedulerClient.called).isTrue();
        assertThat(aiSchedulerClient.requestDTO.getRequiredCount()).isEqualTo(2);
        assertThat(aiSchedulerClient.requestDTO.getCandidates()).extracting(candidate -> candidate.getUserId())
            .containsExactly(101L, 102L, 103L);
        assertThat(detailVO.getAssignments()).extracting(assignment -> assignment.getUserId())
            .containsExactly(103L, 101L);
    }

    @Test
    void autoGenerateSkipsVolunteerWhenAvailabilityDoesNotCoverPositionTime() {
        StubActivityClient activityClient = new StubActivityClient();
        activityClient.positions = List.of(createPosition(20L, 10L, 1, null,
            LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 12, 0)));
        StubUserClient userClient = new StubUserClient();
        userClient.volunteers = List.of(createUser(101L), createUser(102L));
        userClient.availabilities = List.of(
            createAvailability(101L, LocalDateTime.of(2026, 8, 1, 13, 0), LocalDateTime.of(2026, 8, 1, 18, 0)),
            createAvailability(102L, LocalDateTime.of(2026, 8, 1, 8, 0), LocalDateTime.of(2026, 8, 1, 13, 0)));
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(),
            new InMemoryScheduleAssignmentDAO(), new RecordingDomainEventPublisher(), new AllowingScheduleLockManager(),
            activityClient, userClient, new RecordingFreeVolunteerCacheManager());

        ScheduleDetailVO detailVO = service.autoGenerate(createAutoGenerateScheduleDTO());

        assertThat(detailVO.getAssignments()).extracting(assignment -> assignment.getUserId())
            .containsExactly(102L);
    }

    @Test
    void autoGenerateTreatsChineseNoneSkillRequirementAsNoLimit() {
        StubActivityClient activityClient = new StubActivityClient();
        activityClient.positions = List.of(createPosition(20L, 10L, 1, "无",
            LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 12, 0)));
        activityClient.signups = List.of(createSignup(100L, 20L, 101L));
        StubUserClient userClient = new StubUserClient();
        userClient.volunteers = List.of(createUser(101L));
        userClient.skills = List.of(createSkill(101L, "GUIDE"));
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(),
            new InMemoryScheduleAssignmentDAO(), new RecordingDomainEventPublisher(), new AllowingScheduleLockManager(),
            activityClient, userClient, new RecordingFreeVolunteerCacheManager());

        ScheduleDetailVO detailVO = service.autoGenerate(createAutoGenerateScheduleDTO());

        assertThat(detailVO.getAssignments()).extracting(assignment -> assignment.getUserId())
            .containsExactly(101L);
    }

    @Test
    void autoGenerateMatchesSkillCodeIgnoringWhitespaceAndCase() {
        StubActivityClient activityClient = new StubActivityClient();
        activityClient.positions = List.of(createPosition(20L, 10L, 1, " guide ",
            LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 12, 0)));
        activityClient.signups = List.of(createSignup(100L, 20L, 101L));
        StubUserClient userClient = new StubUserClient();
        userClient.volunteers = List.of(createUser(101L));
        userClient.skills = List.of(createSkill(101L, "GUIDE"));
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(),
            new InMemoryScheduleAssignmentDAO(), new RecordingDomainEventPublisher(), new AllowingScheduleLockManager(),
            activityClient, userClient, new RecordingFreeVolunteerCacheManager());

        ScheduleDetailVO detailVO = service.autoGenerate(createAutoGenerateScheduleDTO());

        assertThat(detailVO.getAssignments()).extracting(assignment -> assignment.getUserId())
            .containsExactly(101L);
    }

    @Test
    void autoGenerateMatchesSkillNameWhenPositionUsesChineseSkillName() {
        StubActivityClient activityClient = new StubActivityClient();
        activityClient.positions = List.of(createPosition(20L, 10L, 1, "环境清洁",
            LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 12, 0)));
        activityClient.signups = List.of(createSignup(100L, 20L, 101L));
        StubUserClient userClient = new StubUserClient();
        userClient.volunteers = List.of(createUser(101L));
        userClient.skills = List.of(createSkill(101L, "CLEANING", "环境清洁"));
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(),
            new InMemoryScheduleAssignmentDAO(), new RecordingDomainEventPublisher(), new AllowingScheduleLockManager(),
            activityClient, userClient, new RecordingFreeVolunteerCacheManager());

        ScheduleDetailVO detailVO = service.autoGenerate(createAutoGenerateScheduleDTO());

        assertThat(detailVO.getAssignments()).extracting(assignment -> assignment.getUserId())
            .containsExactly(101L);
    }

    @Test
    void autoGenerateAllowsRegenerationWhenSameActivityAssignmentExists() {
        InMemoryScheduleAssignmentDAO assignmentDAO = new InMemoryScheduleAssignmentDAO();
        ScheduleAssignmentDO existingAssignment = new ScheduleAssignmentDO();
        existingAssignment.setPlanId(99L);
        existingAssignment.setActivityId(100L);
        existingAssignment.setAreaId(10L);
        existingAssignment.setPositionId(20L);
        existingAssignment.setUserId(101L);
        existingAssignment.setWorkDate(LocalDate.of(2026, 8, 1));
        existingAssignment.setStartTime(LocalDateTime.of(2026, 8, 1, 9, 0));
        existingAssignment.setEndTime(LocalDateTime.of(2026, 8, 1, 12, 0));
        existingAssignment.setAssignmentStatus("WAIT_CONFIRM");
        assignmentDAO.insert(existingAssignment);
        StubActivityClient activityClient = new StubActivityClient();
        activityClient.positions = List.of(createPosition(20L, 10L, 1, null,
            LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 12, 0)));
        activityClient.signups = List.of(createSignup(100L, 20L, 101L));
        StubUserClient userClient = new StubUserClient();
        userClient.volunteers = List.of(createUser(101L));
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(), assignmentDAO,
            new RecordingDomainEventPublisher(), new AllowingScheduleLockManager(), activityClient, userClient,
            new RecordingFreeVolunteerCacheManager());

        ScheduleDetailVO detailVO = service.autoGenerate(createAutoGenerateScheduleDTO());

        assertThat(detailVO.getAssignments()).extracting(assignment -> assignment.getUserId())
            .containsExactly(101L);
    }

    @Test
    void autoGenerateOnlyUsesApprovedSignupVolunteersForPosition() {
        StubActivityClient activityClient = new StubActivityClient();
        activityClient.positions = List.of(createPosition(20L, 10L, 1, null,
            LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 12, 0)));
        activityClient.signups = List.of(createSignup(100L, 20L, 102L));
        StubUserClient userClient = new StubUserClient();
        userClient.volunteers = List.of(createUser(101L), createUser(102L));
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(),
            new InMemoryScheduleAssignmentDAO(), new RecordingDomainEventPublisher(), new AllowingScheduleLockManager(),
            activityClient, userClient, new RecordingFreeVolunteerCacheManager());

        ScheduleDetailVO detailVO = service.autoGenerate(createAutoGenerateScheduleDTO());

        assertThat(detailVO.getAssignments()).extracting(assignment -> assignment.getUserId())
            .containsExactly(102L);
    }

    @Test
    void autoGenerateReportsDetailedReasonsWhenPositionCannotBeFilled() {
        InMemoryScheduleAssignmentDAO assignmentDAO = new InMemoryScheduleAssignmentDAO();
        ScheduleAssignmentDO existingAssignment = new ScheduleAssignmentDO();
        existingAssignment.setPlanId(99L);
        existingAssignment.setActivityId(999L);
        existingAssignment.setAreaId(10L);
        existingAssignment.setPositionId(99L);
        existingAssignment.setUserId(103L);
        existingAssignment.setWorkDate(LocalDate.of(2026, 8, 1));
        existingAssignment.setStartTime(LocalDateTime.of(2026, 8, 1, 9, 30));
        existingAssignment.setEndTime(LocalDateTime.of(2026, 8, 1, 11, 0));
        existingAssignment.setAssignmentStatus("WAIT_CONFIRM");
        assignmentDAO.insert(existingAssignment);
        StubActivityClient activityClient = new StubActivityClient();
        activityClient.positions = List.of(createPosition(20L, 10L, 2, "GUIDE",
            LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 12, 0)));
        activityClient.signups = List.of(createSignup(100L, 20L, 101L), createSignup(100L, 20L, 102L),
            createSignup(100L, 20L, 103L));
        StubUserClient userClient = new StubUserClient();
        userClient.volunteers = List.of(createUser(101L, "王文川"), createUser(102L, "刘向阳"),
            createUser(103L, "陈广东"));
        userClient.skills = List.of(createSkill(101L, "GUIDE"), createSkill(102L, "SECURITY"),
            createSkill(103L, "GUIDE"));
        userClient.availabilities = List.of(
            createAvailability(101L, LocalDateTime.of(2026, 8, 1, 13, 0), LocalDateTime.of(2026, 8, 1, 18, 0)),
            createAvailability(102L, LocalDateTime.of(2026, 8, 1, 8, 0), LocalDateTime.of(2026, 8, 1, 13, 0)),
            createAvailability(103L, LocalDateTime.of(2026, 8, 1, 8, 0), LocalDateTime.of(2026, 8, 1, 13, 0)));
        ScheduleService service = new ScheduleServiceImpl(new InMemorySchedulePlanDAO(), assignmentDAO,
            new RecordingDomainEventPublisher(), new AllowingScheduleLockManager(), activityClient, userClient,
            new RecordingFreeVolunteerCacheManager());

        assertThatThrownBy(() -> service.autoGenerate(createAutoGenerateScheduleDTO()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("岗位 20")
            .hasMessageContaining("需要 2 人")
            .hasMessageContaining("可排 0 人")
            .hasMessageContaining("王文川：服务时间不覆盖岗位时间")
            .hasMessageContaining("刘向阳：缺少岗位技能 GUIDE")
            .hasMessageContaining("陈广东：与已有排班时间冲突");
    }

    @Test
    void autoGenerateReturnsExistingPlanWhenAssignmentsAlreadyExistForActivity() {
        InMemorySchedulePlanDAO planDAO = new InMemorySchedulePlanDAO();
        InMemoryScheduleAssignmentDAO assignmentDAO = new InMemoryScheduleAssignmentDAO();
        StubActivityClient activityClient = new StubActivityClient();
        activityClient.positions = List.of(createPosition(20L, 10L, 1, null,
            LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 12, 0)));
        activityClient.signups = List.of(createSignup(100L, 20L, 101L), createSignup(100L, 20L, 102L));
        StubUserClient userClient = new StubUserClient();
        userClient.volunteers = List.of(createUser(101L), createUser(102L));
        ScheduleService service = new ScheduleServiceImpl(planDAO, assignmentDAO, new RecordingDomainEventPublisher(),
            new AllowingScheduleLockManager(), activityClient, userClient, new RecordingFreeVolunteerCacheManager());

        ScheduleDetailVO first = service.autoGenerate(createAutoGenerateScheduleDTO());
        ScheduleDetailVO second = service.autoGenerate(createAutoGenerateScheduleDTO());

        assertThat(second.getPlanId()).isEqualTo(first.getPlanId());
        assertThat(second.getAssignments()).extracting(assignment -> assignment.getUserId())
            .containsExactly(101L);
        assertThat(planDAO.plans).hasSize(1);
        assertThat(assignmentDAO.assignments).hasSize(1);
    }

    @Test
    void supplementAssignmentAddsConfirmedUserToLatestPlan() {
        InMemorySchedulePlanDAO planDAO = new InMemorySchedulePlanDAO();
        InMemoryScheduleAssignmentDAO assignmentDAO = new InMemoryScheduleAssignmentDAO();
        StubActivityClient activityClient = new StubActivityClient();
        PositionDTO positionDTO = createPosition(20L, 10L, 2, "GUIDE",
            LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 12, 0));
        activityClient.positions = List.of(positionDTO);
        StubUserClient userClient = new StubUserClient();
        userClient.volunteers = List.of(createUser(101L), createUser(102L));
        ScheduleService service = new ScheduleServiceImpl(planDAO, assignmentDAO, new RecordingDomainEventPublisher(),
            new AllowingScheduleLockManager(), activityClient, userClient, new RecordingFreeVolunteerCacheManager());
        ScheduleDetailVO first = service.generate(createGenerateScheduleDTO());
        SupplementScheduleAssignmentDTO dto = createSupplementDTO(100L, 10L, 20L, 102L);

        ScheduleDetailVO detailVO = service.supplementAssignment(dto);

        assertThat(detailVO.getPlanId()).isEqualTo(first.getPlanId());
        assertThat(detailVO.getAssignments()).extracting(assignment -> assignment.getUserId())
            .containsExactly(30L, 102L);
        assertThat(detailVO.getAssignments()).filteredOn(assignment -> assignment.getUserId().equals(102L))
            .extracting(ScheduleAssignmentVO::getAssignmentStatus)
            .containsExactly("CONFIRMED");
    }

    private GenerateScheduleDTO createGenerateScheduleDTO() {
        GenerateScheduleDTO dto = new GenerateScheduleDTO();
        dto.setActivityId(100L);
        dto.setPlanName("第一版排班");
        dto.setGeneratedBy(1L);
        dto.setAssignments(List.of(createAssignment(10L, 20L, 30L)));
        return dto;
    }

    private AutoGenerateScheduleDTO createAutoGenerateScheduleDTO() {
        AutoGenerateScheduleDTO dto = new AutoGenerateScheduleDTO();
        dto.setActivityId(100L);
        dto.setPlanName("自动排班");
        dto.setGeneratedBy(1L);
        return dto;
    }

    private ScheduleAssignmentDTO createAssignment(Long areaId, Long positionId, Long userId) {
        ScheduleAssignmentDTO dto = new ScheduleAssignmentDTO();
        dto.setAreaId(areaId);
        dto.setPositionId(positionId);
        dto.setUserId(userId);
        dto.setWorkDate(LocalDate.of(2026, 8, 1));
        dto.setStartTime(LocalDateTime.of(2026, 8, 1, 9, 0));
        dto.setEndTime(LocalDateTime.of(2026, 8, 1, 12, 0));
        return dto;
    }

    private SupplementScheduleAssignmentDTO createSupplementDTO(Long activityId, Long areaId, Long positionId,
                                                                Long userId) {
        SupplementScheduleAssignmentDTO dto = new SupplementScheduleAssignmentDTO();
        dto.setActivityId(activityId);
        dto.setAreaId(areaId);
        dto.setPositionId(positionId);
        dto.setUserId(userId);
        return dto;
    }

    private PositionDTO createPosition(Long id, Long areaId, Integer needCount, String skillRequirement,
                                       LocalDateTime startTime, LocalDateTime endTime) {
        PositionDTO dto = new PositionDTO();
        dto.setId(id);
        dto.setAreaId(areaId);
        dto.setNeedCount(needCount);
        dto.setSkillRequirement(skillRequirement);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        dto.setSalary(BigDecimal.ZERO);
        return dto;
    }

    private UserDTO createUser(Long id) {
        UserDTO dto = new UserDTO();
        dto.setId(id);
        dto.setRealName("志愿者" + id);
        dto.setUserType("VOLUNTEER");
        dto.setAuthStatus("PASSED");
        return dto;
    }

    private UserDTO createUser(Long id, String realName) {
        UserDTO dto = createUser(id);
        dto.setRealName(realName);
        return dto;
    }

    private UserSkillDTO createSkill(Long userId, String skillCode) {
        return createSkill(userId, skillCode, skillCode);
    }

    private UserSkillDTO createSkill(Long userId, String skillCode, String skillName) {
        UserSkillDTO dto = new UserSkillDTO();
        dto.setUserId(userId);
        dto.setSkillCode(skillCode);
        dto.setSkillName(skillName);
        return dto;
    }

    private UserAvailabilityDTO createAvailability(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        UserAvailabilityDTO dto = new UserAvailabilityDTO();
        dto.setUserId(userId);
        dto.setAvailableDate(startTime.toLocalDate());
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        dto.setStatus("AVAILABLE");
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

    private AiScheduleResultDTO createAiScheduleResult(Long positionId, List<Long> userIds) {
        AiScheduleResultDTO resultDTO = new AiScheduleResultDTO();
        resultDTO.setActivityId(100L);
        resultDTO.setPositionId(positionId);
        List<AiRecommendationDTO> recommendations = new ArrayList<>();
        for (int i = 0; i < userIds.size(); i++) {
            AiRecommendationDTO recommendationDTO = new AiRecommendationDTO();
            recommendationDTO.setUserId(userIds.get(i));
            recommendationDTO.setRankNo(i + 1);
            recommendationDTO.setMatchScore(BigDecimal.valueOf(100L - i));
            recommendationDTO.setReason("ai order");
            recommendations.add(recommendationDTO);
        }
        resultDTO.setRecommendations(recommendations);
        return resultDTO;
    }

    private static class InMemorySchedulePlanDAO implements SchedulePlanDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<SchedulePlanDO> plans = new ArrayList<>();

        @Override
        public int insert(SchedulePlanDO planDO) {
            planDO.setId(idGenerator.getAndIncrement());
            plans.add(planDO);
            return 1;
        }

        @Override
        public SchedulePlanDO selectLatestByActivityId(Long activityId) {
            return plans.stream()
                .filter(plan -> plan.getActivityId().equals(activityId))
                .reduce((first, second) -> second)
                .orElse(null);
        }

        @Override
        public SchedulePlanDO selectById(Long id) {
            return plans.stream()
                .filter(plan -> plan.getId().equals(id))
                .findFirst()
                .orElse(null);
        }

        @Override
        public int updateStatus(SchedulePlanDO planDO) {
            SchedulePlanDO oldPlan = selectById(planDO.getId());
            if (oldPlan == null) {
                return 0;
            }
            oldPlan.setPlanStatus(planDO.getPlanStatus());
            oldPlan.setPublishedTime(planDO.getPublishedTime());
            return 1;
        }
    }

    private static class InMemoryScheduleAssignmentDAO implements ScheduleAssignmentDAO {

        private final AtomicLong idGenerator = new AtomicLong(1L);
        private final List<ScheduleAssignmentDO> assignments = new ArrayList<>();

        @Override
        public int insert(ScheduleAssignmentDO assignmentDO) {
            assignmentDO.setId(idGenerator.getAndIncrement());
            assignments.add(assignmentDO);
            return 1;
        }

        @Override
        public List<ScheduleAssignmentDO> selectByPlanId(Long planId) {
            return assignments.stream()
                .filter(assignment -> assignment.getPlanId().equals(planId))
                .toList();
        }

        @Override
        public List<ScheduleAssignmentDO> selectByUserId(Long userId) {
            return assignments.stream()
                .filter(assignment -> assignment.getUserId().equals(userId))
                .toList();
        }

        @Override
        public ScheduleAssignmentDO selectById(Long id) {
            return assignments.stream()
                .filter(assignment -> assignment.getId().equals(id))
                .findFirst()
                .orElse(null);
        }

        @Override
        public int updateStatus(ScheduleAssignmentDO assignmentDO) {
            ScheduleAssignmentDO oldAssignment = selectById(assignmentDO.getId());
            if (oldAssignment == null) {
                return 0;
            }
            oldAssignment.setAssignmentStatus(assignmentDO.getAssignmentStatus());
            return 1;
        }

        @Override
        public int countUserTimeConflict(ScheduleAssignmentDO assignmentDO) {
            return (int) assignments.stream()
                .filter(assignment -> assignment.getActivityId().equals(assignmentDO.getActivityId()))
                .filter(assignment -> assignment.getUserId().equals(assignmentDO.getUserId()))
                .filter(assignment -> assignment.getWorkDate().equals(assignmentDO.getWorkDate()))
                .filter(assignment -> assignment.getStartTime().isBefore(assignmentDO.getEndTime())
                    && assignmentDO.getStartTime().isBefore(assignment.getEndTime()))
                .count();
        }
    }

    private static class FailingScheduleAssignmentDAO extends InMemoryScheduleAssignmentDAO {

        @Override
        public int countUserTimeConflict(ScheduleAssignmentDO assignmentDO) {
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

    private static class AllowingScheduleLockManager implements ScheduleLockManager {

        private final List<Long> releasedActivityIds = new ArrayList<>();

        @Override
        public boolean tryLock(Long activityId) {
            return true;
        }

        @Override
        public void unlock(Long activityId) {
            releasedActivityIds.add(activityId);
        }
    }

    private static class DenyingScheduleLockManager implements ScheduleLockManager {

        private final List<Long> releasedActivityIds = new ArrayList<>();

        @Override
        public boolean tryLock(Long activityId) {
            return false;
        }

        @Override
        public void unlock(Long activityId) {
            releasedActivityIds.add(activityId);
        }
    }

    private static class RecordingFreeVolunteerCacheManager implements FreeVolunteerCacheManager {

        private List<Long> cachedLookupUserIds = Collections.emptyList();
        private final List<Long> cachedUserIds = new ArrayList<>();

        @Override
        public void cacheFreeVolunteerIds(LocalDate date, List<Long> userIds) {
            cachedUserIds.addAll(userIds);
        }

        @Override
        public List<Long> listFreeVolunteerIds(LocalDate date) {
            return cachedLookupUserIds;
        }
    }

    private static class StubActivityClient implements ActivityClient {

        private List<PositionDTO> positions = Collections.emptyList();
        private List<ActivitySignupDTO> signups = Collections.emptyList();

        @Override
        public Result<List<PositionDTO>> getPositionList(Long activityId) {
            return Result.success(positions);
        }

        @Override
        public Result<PositionDTO> getPosition(Long id) {
            return Result.success(positions.stream()
                .filter(position -> id.equals(position.getId()))
                .findFirst()
                .orElse(null));
        }

        @Override
        public Result<List<AreaDTO>> getAreaList(Long activityId) {
            return Result.success(Collections.emptyList());
        }

        @Override
        public Result<List<ActivitySignupDTO>> listSignups(Long activityId, String signupStatus) {
            if (!signups.isEmpty()) {
                return Result.success(signups);
            }
            return Result.success(positions.stream()
                .flatMap(position -> List.of(createDefaultSignup(activityId, position.getId(), 101L),
                    createDefaultSignup(activityId, position.getId(), 102L),
                    createDefaultSignup(activityId, position.getId(), 103L)).stream())
                .toList());
        }

        private ActivitySignupDTO createDefaultSignup(Long activityId, Long positionId, Long userId) {
            ActivitySignupDTO dto = new ActivitySignupDTO();
            dto.setActivityId(activityId);
            dto.setPositionId(positionId);
            dto.setUserId(userId);
            dto.setSignupStatus("APPROVED");
            return dto;
        }
    }

    private static class StubUserClient implements UserClient {

        private boolean listVolunteersCalled;
        private List<UserDTO> volunteers = Collections.emptyList();
        private List<UserSkillDTO> skills = Collections.emptyList();
        private List<UserAvailabilityDTO> availabilities = Collections.emptyList();

        @Override
        public Result<List<UserDTO>> listVolunteers() {
            listVolunteersCalled = true;
            return Result.success(volunteers);
        }

        @Override
        public Result<List<UserSkillDTO>> listSkills(Long id) {
            return Result.success(skills.stream()
                .filter(skill -> id.equals(skill.getUserId()))
                .toList());
        }

        @Override
        public Result<List<UserAvailabilityDTO>> listAvailability(Long id) {
            if (availabilities.isEmpty()) {
                return Result.success(List.of(createDefaultAvailability(id)));
            }
            return Result.success(availabilities.stream()
                .filter(availability -> id.equals(availability.getUserId()))
                .toList());
        }

        @Override
        public Result<UserDTO> getById(Long id) {
            return Result.success(volunteers.stream()
                .filter(user -> id.equals(user.getId()))
                .findFirst()
                .orElse(null));
        }

        private UserAvailabilityDTO createDefaultAvailability(Long userId) {
            UserAvailabilityDTO dto = new UserAvailabilityDTO();
            dto.setUserId(userId);
            dto.setAvailableDate(LocalDate.of(2026, 8, 1));
            dto.setStartTime(LocalDateTime.of(2026, 8, 1, 0, 0));
            dto.setEndTime(LocalDateTime.of(2026, 8, 1, 23, 59));
            dto.setStatus("AVAILABLE");
            return dto;
        }
    }

    private static class StubAiSchedulerClient implements AiSchedulerClient {

        private boolean called;
        private AiScheduleRequestDTO requestDTO;
        private AiScheduleResultDTO resultDTO;

        @Override
        public Result<AiScheduleResultDTO> recommend(AiScheduleRequestDTO requestDTO) {
            called = true;
            this.requestDTO = requestDTO;
            return Result.success(resultDTO);
        }

        @Override
        public Result<AiPredictionResultDTO> predictRisks(AiPredictionRequestDTO requestDTO) {
            return Result.success(new AiPredictionResultDTO());
        }
    }
}
