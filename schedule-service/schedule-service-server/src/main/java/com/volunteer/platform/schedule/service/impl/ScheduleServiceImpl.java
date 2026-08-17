package com.volunteer.platform.schedule.service.impl;

import com.volunteer.platform.activity.client.api.ActivityClient;
import com.volunteer.platform.activity.client.dto.ActivitySignupDTO;
import com.volunteer.platform.activity.client.dto.PositionDTO;
import com.volunteer.platform.ai.client.api.AiSchedulerClient;
import com.volunteer.platform.ai.client.dto.AiCandidateDTO;
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
import com.volunteer.platform.schedule.entity.ScheduleAssignmentDO;
import com.volunteer.platform.schedule.entity.SchedulePlanDO;
import com.volunteer.platform.schedule.client.dto.SupplementScheduleAssignmentDTO;
import com.volunteer.platform.schedule.manager.FreeVolunteerCacheManager;
import com.volunteer.platform.schedule.manager.ScheduleLockManager;
import com.volunteer.platform.schedule.service.ScheduleService;
import com.volunteer.platform.schedule.vo.ScheduleAssignmentVO;
import com.volunteer.platform.schedule.vo.ScheduleDetailVO;
import com.volunteer.platform.user.client.api.UserClient;
import com.volunteer.platform.user.client.dto.UserAvailabilityDTO;
import com.volunteer.platform.user.client.dto.UserDTO;
import com.volunteer.platform.user.client.dto.UserSkillDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private static final Integer NOT_FOUND_CODE = 404;
    private static final String GENERATED_STATUS = "GENERATED";
    private static final String PUBLISHED_STATUS = "PUBLISHED";
    private static final String WAIT_CONFIRM_STATUS = "WAIT_CONFIRM";
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    private static final String SIGNUP_APPROVED_STATUS = "APPROVED";

    private final SchedulePlanDAO schedulePlanDAO;
    private final ScheduleAssignmentDAO scheduleAssignmentDAO;
    private final DomainEventPublisher domainEventPublisher;
    private final ScheduleLockManager scheduleLockManager;
    private final ActivityClient activityClient;
    private final UserClient userClient;
    private final FreeVolunteerCacheManager freeVolunteerCacheManager;
    private final AiSchedulerClient aiSchedulerClient;

    public ScheduleServiceImpl(SchedulePlanDAO schedulePlanDAO, ScheduleAssignmentDAO scheduleAssignmentDAO,
                               DomainEventPublisher domainEventPublisher) {
        this(schedulePlanDAO, scheduleAssignmentDAO, domainEventPublisher, null);
    }

    public ScheduleServiceImpl(SchedulePlanDAO schedulePlanDAO, ScheduleAssignmentDAO scheduleAssignmentDAO,
                               DomainEventPublisher domainEventPublisher, ScheduleLockManager scheduleLockManager) {
        this(schedulePlanDAO, scheduleAssignmentDAO, domainEventPublisher, scheduleLockManager, null, null, null);
    }

    public ScheduleServiceImpl(SchedulePlanDAO schedulePlanDAO, ScheduleAssignmentDAO scheduleAssignmentDAO,
                               DomainEventPublisher domainEventPublisher, ScheduleLockManager scheduleLockManager,
                               ActivityClient activityClient, UserClient userClient,
                               FreeVolunteerCacheManager freeVolunteerCacheManager) {
        this(schedulePlanDAO, scheduleAssignmentDAO, domainEventPublisher, scheduleLockManager, activityClient,
            userClient, freeVolunteerCacheManager, null);
    }

    @Autowired
    public ScheduleServiceImpl(SchedulePlanDAO schedulePlanDAO, ScheduleAssignmentDAO scheduleAssignmentDAO,
                               DomainEventPublisher domainEventPublisher, ScheduleLockManager scheduleLockManager,
                               ActivityClient activityClient, UserClient userClient,
                               FreeVolunteerCacheManager freeVolunteerCacheManager,
                               AiSchedulerClient aiSchedulerClient) {
        this.schedulePlanDAO = schedulePlanDAO;
        this.scheduleAssignmentDAO = scheduleAssignmentDAO;
        this.domainEventPublisher = domainEventPublisher;
        this.scheduleLockManager = scheduleLockManager;
        this.activityClient = activityClient;
        this.userClient = userClient;
        this.freeVolunteerCacheManager = freeVolunteerCacheManager;
        this.aiSchedulerClient = aiSchedulerClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleDetailVO generate(GenerateScheduleDTO dto) {
        return generate(dto, true);
    }

    private ScheduleDetailVO generate(GenerateScheduleDTO dto, boolean validateExistingConflict) {
        if (!tryLock(dto.getActivityId())) {
            throw new BusinessException(409, "schedule generation in progress");
        }
        try {
            return doGenerate(dto, validateExistingConflict);
        } finally {
            unlock(dto.getActivityId());
        }
    }

    private ScheduleDetailVO doGenerate(GenerateScheduleDTO dto, boolean validateExistingConflict) {
        validateTimeConflict(dto.getAssignments());

        SchedulePlanDO planDO = new SchedulePlanDO();
        planDO.setActivityId(dto.getActivityId());
        planDO.setPlanNo(buildPlanNo(dto.getActivityId()));
        planDO.setPlanName(dto.getPlanName());
        planDO.setPlanStatus(GENERATED_STATUS);
        planDO.setGeneratedBy(dto.getGeneratedBy());
        schedulePlanDAO.insert(planDO);

        for (ScheduleAssignmentDTO assignmentDTO : dto.getAssignments()) {
            ScheduleAssignmentDO assignmentDO = toAssignmentDO(dto.getActivityId(), planDO.getId(), assignmentDTO);
            if (validateExistingConflict) {
                validateExistingTimeConflict(assignmentDO);
            }
            scheduleAssignmentDAO.insert(assignmentDO);
        }
        return buildDetail(planDO);
    }

    @Override
    public ScheduleDetailVO autoGenerate(AutoGenerateScheduleDTO dto) {
        validateAutoGenerate(dto);
        if (!tryLock(dto.getActivityId())) {
            throw new BusinessException(409, "schedule generation in progress");
        }
        try {
            ScheduleDetailVO existingDetail = findExistingScheduleDetail(dto.getActivityId());
            if (existingDetail != null) {
                return existingDetail;
            }
            List<PositionDTO> positions = listPositions(dto.getActivityId());
            List<ActivitySignupDTO> signups = listApprovedSignups(dto.getActivityId());
            List<UserDTO> volunteers = listFreeVolunteers(positions, signups);
            GenerateScheduleDTO generateDTO = new GenerateScheduleDTO();
            generateDTO.setActivityId(dto.getActivityId());
            generateDTO.setPlanName(dto.getPlanName());
            generateDTO.setGeneratedBy(dto.getGeneratedBy());
            generateDTO.setAssignments(buildAssignments(dto.getActivityId(), positions, volunteers, signups));
            return doGenerate(generateDTO, false);
        } finally {
            unlock(dto.getActivityId());
        }
    }

    private ScheduleDetailVO findExistingScheduleDetail(Long activityId) {
        SchedulePlanDO latestPlan = schedulePlanDAO.selectLatestByActivityId(activityId);
        if (latestPlan == null) {
            return null;
        }
        List<ScheduleAssignmentDO> assignments = scheduleAssignmentDAO.selectByPlanId(latestPlan.getId());
        if (assignments.isEmpty()) {
            return null;
        }
        return buildDetail(latestPlan, assignments);
    }

    private boolean tryLock(Long activityId) {
        return scheduleLockManager == null || scheduleLockManager.tryLock(activityId);
    }

    private void unlock(Long activityId) {
        if (scheduleLockManager != null) {
            scheduleLockManager.unlock(activityId);
        }
    }

    private void validateAutoGenerate(AutoGenerateScheduleDTO dto) {
        if (dto == null || dto.getActivityId() == null) {
            throw new BusinessException(400, "auto schedule target is required");
        }
        if (activityClient == null || userClient == null) {
            throw new BusinessException(500, "auto schedule client is required");
        }
    }

    private List<PositionDTO> listPositions(Long activityId) {
        Result<List<PositionDTO>> result = activityClient.getPositionList(activityId);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BusinessException(404, "position list not found");
        }
        return result.getData().stream()
            .filter(position -> position.getId() != null)
            .filter(position -> position.getAreaId() != null)
            .filter(position -> position.getNeedCount() != null && position.getNeedCount() > 0)
            .filter(position -> position.getStartTime() != null && position.getEndTime() != null)
            .toList();
    }

    private List<UserDTO> listFreeVolunteers(List<PositionDTO> positions, List<ActivitySignupDTO> signups) {
        Result<List<UserDTO>> result = userClient.listVolunteers();
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BusinessException(404, "volunteer list not found");
        }
        Set<Long> signupUserIds = signups.stream()
            .map(ActivitySignupDTO::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        List<UserDTO> volunteers = result.getData().stream()
            .filter(user -> user.getId() != null)
            .filter(user -> signupUserIds.contains(user.getId()))
            .toList();
        cacheFreeUserIds(positions, volunteers);
        return volunteers;
    }

    private List<ActivitySignupDTO> listApprovedSignups(Long activityId) {
        Result<List<ActivitySignupDTO>> result = activityClient.listSignups(activityId, SIGNUP_APPROVED_STATUS);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BusinessException(404, "approved signup list not found");
        }
        List<ActivitySignupDTO> signups = result.getData().stream()
            .filter(signup -> signup.getUserId() != null)
            .filter(signup -> signup.getPositionId() == null || signup.getPositionId() > 0)
            .toList();
        if (signups.isEmpty()) {
            throw new BusinessException(404, "no approved signup volunteer for schedule");
        }
        return signups;
    }

    private void cacheFreeUserIds(List<PositionDTO> positions, List<UserDTO> volunteers) {
        if (freeVolunteerCacheManager == null || positions.isEmpty()) {
            return;
        }
        freeVolunteerCacheManager.cacheFreeVolunteerIds(positions.get(0).getStartTime().toLocalDate(),
            volunteers.stream().map(UserDTO::getId).toList());
    }

    private List<ScheduleAssignmentDTO> buildAssignments(Long activityId, List<PositionDTO> positions,
                                                         List<UserDTO> volunteers,
                                                         List<ActivitySignupDTO> signups) {
        List<ScheduleAssignmentDTO> assignments = new ArrayList<>();
        for (PositionDTO position : positions) {
            int assignedCount = 0;
            for (UserDTO volunteer : sortVolunteersForPosition(activityId, position, volunteers, signups)) {
                if (assignedCount >= position.getNeedCount()) {
                    break;
                }
                ScheduleAssignmentDTO assignmentDTO = toAutoAssignmentDTO(position, volunteer.getId());
                if (hasTimeConflict(assignments, assignmentDTO) || hasExistingTimeConflict(activityId, assignmentDTO)) {
                    continue;
                }
                assignments.add(assignmentDTO);
                assignedCount++;
            }
            if (assignedCount < position.getNeedCount()) {
                throw new BusinessException(404,
                    buildAutoScheduleFailureMessage(activityId, position, volunteers, signups, assignments,
                        assignedCount));
            }
        }
        if (assignments.isEmpty()) {
            throw new BusinessException(404, "no available volunteer for schedule");
        }
        return assignments;
    }

    private String buildAutoScheduleFailureMessage(Long activityId, PositionDTO position, List<UserDTO> volunteers,
                                                   List<ActivitySignupDTO> signups,
                                                   List<ScheduleAssignmentDTO> assignments, int assignedCount) {
        List<String> reasons = volunteers.stream()
            .map(volunteer -> buildVolunteerBlockReason(activityId, position, volunteer, signups, assignments))
            .filter(Objects::nonNull)
            .limit(8)
            .toList();
        String detail = reasons.isEmpty() ? "暂无可用志愿者，请检查报名审核、技能标签、服务时间或历史排班。"
            : String.join("；", reasons);
        return "岗位 " + positionLabel(position) + " 自动排班失败：需要 " + position.getNeedCount()
            + " 人，可排 " + assignedCount + " 人。未排原因：" + detail;
    }

    private String buildVolunteerBlockReason(Long activityId, PositionDTO position, UserDTO volunteer,
                                             List<ActivitySignupDTO> signups,
                                             List<ScheduleAssignmentDTO> assignments) {
        Long userId = volunteer.getId();
        if (!hasApprovedSignup(userId, position, signups)) {
            return volunteerLabel(volunteer) + "：未通过该岗位报名";
        }
        if (!isAvailableForPosition(userId, position)) {
            return volunteerLabel(volunteer) + "：服务时间不覆盖岗位时间";
        }
        if (!matchesSkill(userId, position.getSkillRequirement())) {
            return volunteerLabel(volunteer) + "：缺少岗位技能 " + position.getSkillRequirement().trim();
        }
        ScheduleAssignmentDTO assignmentDTO = toAutoAssignmentDTO(position, userId);
        if (hasTimeConflict(assignments, assignmentDTO)) {
            return volunteerLabel(volunteer) + "：与本次排班时间冲突";
        }
        if (hasExistingTimeConflict(activityId, assignmentDTO)) {
            return volunteerLabel(volunteer) + "：与已有排班时间冲突";
        }
        return null;
    }

    private String positionLabel(PositionDTO position) {
        if (position.getName() == null || position.getName().isBlank()) {
            return String.valueOf(position.getId());
        }
        return position.getName() + "（" + position.getId() + "）";
    }

    private String volunteerLabel(UserDTO volunteer) {
        if (volunteer.getRealName() != null && !volunteer.getRealName().isBlank()) {
            return volunteer.getRealName();
        }
        if (volunteer.getUsername() != null && !volunteer.getUsername().isBlank()) {
            return volunteer.getUsername();
        }
        return "志愿者 " + volunteer.getId();
    }

    private List<UserDTO> sortVolunteersForPosition(Long activityId, PositionDTO position, List<UserDTO> volunteers,
                                                    List<ActivitySignupDTO> signups) {
        List<UserDTO> skillMatchedVolunteers = volunteers.stream()
            .filter(volunteer -> hasApprovedSignup(volunteer.getId(), position, signups))
            .filter(volunteer -> isAvailableForPosition(volunteer.getId(), position))
            .filter(volunteer -> matchesSkill(volunteer.getId(), position.getSkillRequirement()))
            .toList();
        List<Long> recommendedUserIds = recommendUserIds(activityId, position, skillMatchedVolunteers);
        if (recommendedUserIds.isEmpty()) {
            return skillMatchedVolunteers;
        }
        Map<Long, UserDTO> volunteerMap = skillMatchedVolunteers.stream()
            .collect(Collectors.toMap(UserDTO::getId, Function.identity(), (oldValue, newValue) -> oldValue));
        Set<Long> recommendedUserIdSet = recommendedUserIds.stream().collect(Collectors.toSet());
        List<UserDTO> sortedVolunteers = new ArrayList<>();
        recommendedUserIds.stream()
            .map(volunteerMap::get)
            .filter(Objects::nonNull)
            .forEach(sortedVolunteers::add);
        skillMatchedVolunteers.stream()
            .filter(volunteer -> !recommendedUserIdSet.contains(volunteer.getId()))
            .sorted(Comparator.comparing(UserDTO::getId))
            .forEach(sortedVolunteers::add);
        return sortedVolunteers;
    }

    private boolean hasApprovedSignup(Long userId, PositionDTO position, List<ActivitySignupDTO> signups) {
        return signups.stream()
            .filter(signup -> userId.equals(signup.getUserId()))
            .anyMatch(signup -> signup.getPositionId() == null || signup.getPositionId().equals(position.getId()));
    }

    private List<Long> recommendUserIds(Long activityId, PositionDTO position, List<UserDTO> volunteers) {
        if (aiSchedulerClient == null || volunteers.isEmpty()) {
            return List.of();
        }
        AiScheduleRequestDTO requestDTO = new AiScheduleRequestDTO();
        requestDTO.setActivityId(activityId);
        requestDTO.setPositionId(position.getId());
        requestDTO.setRequiredCount(position.getNeedCount());
        requestDTO.setCandidates(volunteers.stream().map(this::toAiCandidateDTO).toList());
        try {
            Result<AiScheduleResultDTO> result = aiSchedulerClient.recommend(requestDTO);
            if (result == null || !result.isSuccess() || result.getData() == null
                || result.getData().getRecommendations() == null) {
                return List.of();
            }
            return result.getData().getRecommendations().stream()
                .filter(recommendation -> recommendation.getUserId() != null)
                .sorted(Comparator.comparing(AiRecommendationDTO::getRankNo,
                    Comparator.nullsLast(Comparator.naturalOrder())))
                .map(AiRecommendationDTO::getUserId)
                .distinct()
                .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private AiCandidateDTO toAiCandidateDTO(UserDTO userDTO) {
        AiCandidateDTO candidateDTO = new AiCandidateDTO();
        candidateDTO.setUserId(userDTO.getId());
        candidateDTO.setAvailable(Boolean.TRUE);
        candidateDTO.setHistoricalScore(BigDecimal.valueOf(60L));
        candidateDTO.setSkillScore(BigDecimal.valueOf(100L));
        return candidateDTO;
    }

    private boolean matchesSkill(Long userId, String skillRequirement) {
        if (isNoSkillLimit(skillRequirement)) {
            return true;
        }
        String normalizedRequirement = skillRequirement.trim();
        Result<List<UserSkillDTO>> result = userClient.listSkills(userId);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            return false;
        }
        return result.getData().stream()
            .anyMatch(skill -> matchesSkillValue(normalizedRequirement, skill.getSkillCode())
                || matchesSkillValue(normalizedRequirement, skill.getSkillName()));
    }

    private boolean matchesSkillValue(String normalizedRequirement, String skillValue) {
        return skillValue != null
            && !skillValue.trim().isBlank()
            && normalizedRequirement.equalsIgnoreCase(skillValue.trim());
    }

    private boolean isNoSkillLimit(String skillRequirement) {
        if (skillRequirement == null || skillRequirement.isBlank()) {
            return true;
        }
        String normalizedRequirement = skillRequirement.trim();
        return "无".equals(normalizedRequirement)
            || "不限".equals(normalizedRequirement)
            || "不限技能".equals(normalizedRequirement)
            || "ANY".equalsIgnoreCase(normalizedRequirement)
            || "NONE".equalsIgnoreCase(normalizedRequirement);
    }

    private boolean isAvailableForPosition(Long userId, PositionDTO position) {
        Result<List<UserAvailabilityDTO>> result = userClient.listAvailability(userId);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            return false;
        }
        return result.getData().stream()
            .filter(availability -> "AVAILABLE".equals(availability.getStatus()))
            .filter(availability -> position.getStartTime().toLocalDate().equals(availability.getAvailableDate()))
            .anyMatch(availability -> covers(availability, position));
    }

    private boolean covers(UserAvailabilityDTO availability, PositionDTO position) {
        return availability.getStartTime() != null
            && availability.getEndTime() != null
            && !availability.getStartTime().isAfter(position.getStartTime())
            && !availability.getEndTime().isBefore(position.getEndTime());
    }

    private boolean hasTimeConflict(List<ScheduleAssignmentDTO> assignments, ScheduleAssignmentDTO assignmentDTO) {
        return assignments.stream()
            .anyMatch(oldAssignment -> isSameUserSameDay(oldAssignment, assignmentDTO)
                && isTimeOverlap(oldAssignment, assignmentDTO));
    }

    private boolean hasExistingTimeConflict(Long activityId, ScheduleAssignmentDTO assignmentDTO) {
        return scheduleAssignmentDAO.selectByUserId(assignmentDTO.getUserId()).stream()
            .filter(assignment -> !Objects.equals(activityId, assignment.getActivityId()))
            .filter(assignment -> assignmentDTO.getWorkDate().equals(assignment.getWorkDate()))
            .anyMatch(assignment -> assignment.getStartTime().isBefore(assignmentDTO.getEndTime())
                && assignmentDTO.getStartTime().isBefore(assignment.getEndTime()));
    }

    private ScheduleAssignmentDTO toAutoAssignmentDTO(PositionDTO position, Long userId) {
        ScheduleAssignmentDTO dto = new ScheduleAssignmentDTO();
        dto.setAreaId(position.getAreaId());
        dto.setPositionId(position.getId());
        dto.setUserId(userId);
        dto.setWorkDate(position.getStartTime().toLocalDate());
        dto.setStartTime(position.getStartTime());
        dto.setEndTime(position.getEndTime());
        return dto;
    }

    private void validateTimeConflict(List<ScheduleAssignmentDTO> assignments) {
        for (int i = 0; i < assignments.size(); i++) {
            for (int j = i + 1; j < assignments.size(); j++) {
                ScheduleAssignmentDTO first = assignments.get(i);
                ScheduleAssignmentDTO second = assignments.get(j);
                if (isSameUserSameDay(first, second) && isTimeOverlap(first, second)) {
                    throw new BusinessException(409, "schedule time conflict");
                }
            }
        }
    }

    private boolean isSameUserSameDay(ScheduleAssignmentDTO first, ScheduleAssignmentDTO second) {
        return first.getUserId().equals(second.getUserId())
            && first.getWorkDate().equals(second.getWorkDate());
    }

    private boolean isTimeOverlap(ScheduleAssignmentDTO first, ScheduleAssignmentDTO second) {
        return first.getStartTime().isBefore(second.getEndTime())
            && second.getStartTime().isBefore(first.getEndTime());
    }

    private void validateExistingTimeConflict(ScheduleAssignmentDO assignmentDO) {
        if (scheduleAssignmentDAO.countUserTimeConflict(assignmentDO) > 0) {
            throw new BusinessException(409, "schedule time conflict");
        }
    }

    @Override
    public ScheduleDetailVO getActivityDetail(Long activityId) {
        SchedulePlanDO planDO = schedulePlanDAO.selectLatestByActivityId(activityId);
        if (planDO == null) {
            throw new BusinessException(NOT_FOUND_CODE, "schedule plan not found");
        }
        return buildDetail(planDO);
    }

    @Override
    public List<ScheduleAssignmentVO> listUserAssignments(Long userId) {
        if (userId == null) {
            throw new BusinessException(400, "user id is required");
        }
        return scheduleAssignmentDAO.selectByUserId(userId).stream()
            .map(this::toAssignmentVO)
            .toList();
    }

    @Override
    public ScheduleDetailVO publish(Long planId) {
        SchedulePlanDO planDO = schedulePlanDAO.selectById(planId);
        if (planDO == null) {
            throw new BusinessException(NOT_FOUND_CODE, "schedule plan not found");
        }
        if (PUBLISHED_STATUS.equals(planDO.getPlanStatus())) {
            return buildDetail(planDO);
        }
        SchedulePlanDO updateDO = new SchedulePlanDO();
        updateDO.setId(planId);
        updateDO.setPlanStatus(PUBLISHED_STATUS);
        updateDO.setPublishedTime(LocalDateTime.now());
        schedulePlanDAO.updateStatus(updateDO);
        domainEventPublisher.publish("schedule-topic", "SCHEDULE_PUBLISHED", "schedule-" + planId,
            Map.of("planId", planId, "activityId", planDO.getActivityId()));
        return buildDetail(schedulePlanDAO.selectById(planId));
    }

    @Override
    public void confirmAssignment(Long assignmentId) {
        if (scheduleAssignmentDAO.selectById(assignmentId) == null) {
            throw new BusinessException(NOT_FOUND_CODE, "schedule assignment not found");
        }
        ScheduleAssignmentDO assignmentDO = new ScheduleAssignmentDO();
        assignmentDO.setId(assignmentId);
        assignmentDO.setAssignmentStatus(CONFIRMED_STATUS);
        scheduleAssignmentDAO.updateStatus(assignmentDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleDetailVO supplementAssignment(SupplementScheduleAssignmentDTO dto) {
        validateSupplementAssignment(dto);
        SchedulePlanDO latestPlan = schedulePlanDAO.selectLatestByActivityId(dto.getActivityId());
        if (latestPlan == null) {
            throw new BusinessException(NOT_FOUND_CODE, "schedule plan not found");
        }
        PositionDTO position = queryPosition(dto.getActivityId(), dto.getPositionId());
        ScheduleAssignmentDTO assignmentDTO = toAutoAssignmentDTO(position, dto.getUserId());
        assignmentDTO.setAreaId(dto.getAreaId() == null ? position.getAreaId() : dto.getAreaId());
        if (hasAnyExistingTimeConflict(assignmentDTO)) {
            throw new BusinessException(409, "volunteer already has schedule in this time range");
        }
        ScheduleAssignmentDO assignmentDO = toAssignmentDO(dto.getActivityId(), latestPlan.getId(), assignmentDTO);
        assignmentDO.setAssignmentStatus(CONFIRMED_STATUS);
        scheduleAssignmentDAO.insert(assignmentDO);
        return buildDetail(latestPlan);
    }

    private void validateSupplementAssignment(SupplementScheduleAssignmentDTO dto) {
        if (dto == null || dto.getActivityId() == null || dto.getPositionId() == null || dto.getUserId() == null) {
            throw new BusinessException(400, "supplement assignment target is required");
        }
        if (activityClient == null) {
            throw new BusinessException(500, "activity client is required");
        }
    }

    private PositionDTO queryPosition(Long activityId, Long positionId) {
        Result<List<PositionDTO>> result = activityClient.getPositionList(activityId);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BusinessException(NOT_FOUND_CODE, "position not found");
        }
        PositionDTO position = result.getData().stream()
            .filter(item -> positionId.equals(item.getId()))
            .findFirst()
            .orElseThrow(() -> new BusinessException(NOT_FOUND_CODE, "position not found"));
        if (position.getId() == null || position.getStartTime() == null || position.getEndTime() == null
            || position.getAreaId() == null) {
            throw new BusinessException(400, "position schedule time is incomplete");
        }
        return position;
    }

    private boolean hasAnyExistingTimeConflict(ScheduleAssignmentDTO assignmentDTO) {
        return scheduleAssignmentDAO.selectByUserId(assignmentDTO.getUserId()).stream()
            .filter(assignment -> assignmentDTO.getWorkDate().equals(assignment.getWorkDate()))
            .anyMatch(assignment -> assignment.getStartTime().isBefore(assignmentDTO.getEndTime())
                && assignmentDTO.getStartTime().isBefore(assignment.getEndTime()));
    }

    private String buildPlanNo(Long activityId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return "SCH" + activityId + timestamp;
    }

    private ScheduleAssignmentDO toAssignmentDO(Long activityId, Long planId, ScheduleAssignmentDTO dto) {
        ScheduleAssignmentDO assignmentDO = new ScheduleAssignmentDO();
        assignmentDO.setPlanId(planId);
        assignmentDO.setActivityId(activityId);
        assignmentDO.setAreaId(dto.getAreaId());
        assignmentDO.setPositionId(dto.getPositionId());
        assignmentDO.setUserId(dto.getUserId());
        assignmentDO.setWorkDate(dto.getWorkDate());
        assignmentDO.setStartTime(dto.getStartTime());
        assignmentDO.setEndTime(dto.getEndTime());
        assignmentDO.setAssignmentStatus(WAIT_CONFIRM_STATUS);
        return assignmentDO;
    }

    private ScheduleDetailVO buildDetail(SchedulePlanDO planDO) {
        return buildDetail(planDO, scheduleAssignmentDAO.selectByPlanId(planDO.getId()));
    }

    private ScheduleDetailVO buildDetail(SchedulePlanDO planDO, List<ScheduleAssignmentDO> assignments) {
        ScheduleDetailVO detailVO = new ScheduleDetailVO();
        detailVO.setPlanId(planDO.getId());
        detailVO.setActivityId(planDO.getActivityId());
        detailVO.setPlanNo(planDO.getPlanNo());
        detailVO.setPlanName(planDO.getPlanName());
        detailVO.setPlanStatus(planDO.getPlanStatus());
        detailVO.setAssignments(assignments.stream()
            .map(this::toAssignmentVO)
            .toList());
        return detailVO;
    }

    private ScheduleAssignmentVO toAssignmentVO(ScheduleAssignmentDO assignmentDO) {
        ScheduleAssignmentVO assignmentVO = new ScheduleAssignmentVO();
        assignmentVO.setId(assignmentDO.getId());
        assignmentVO.setActivityId(assignmentDO.getActivityId());
        assignmentVO.setAreaId(assignmentDO.getAreaId());
        assignmentVO.setPositionId(assignmentDO.getPositionId());
        assignmentVO.setUserId(assignmentDO.getUserId());
        assignmentVO.setWorkDate(assignmentDO.getWorkDate());
        assignmentVO.setStartTime(assignmentDO.getStartTime());
        assignmentVO.setEndTime(assignmentDO.getEndTime());
        assignmentVO.setAssignmentStatus(assignmentDO.getAssignmentStatus());
        return assignmentVO;
    }
}
