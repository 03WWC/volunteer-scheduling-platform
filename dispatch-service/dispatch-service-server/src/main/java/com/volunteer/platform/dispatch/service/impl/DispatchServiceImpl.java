package com.volunteer.platform.dispatch.service.impl;

import com.volunteer.platform.ai.client.api.AiSchedulerClient;
import com.volunteer.platform.ai.client.dto.AiCandidateDTO;
import com.volunteer.platform.ai.client.dto.AiRecommendationDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleRequestDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleResultDTO;
import com.volunteer.platform.common.api.Result;
import com.volunteer.platform.common.exception.BusinessException;
import com.volunteer.platform.dispatch.dao.DispatchRecommendationDAO;
import com.volunteer.platform.dispatch.dao.DispatchTaskDAO;
import com.volunteer.platform.dispatch.dto.DetectShortageDTO;
import com.volunteer.platform.dispatch.dto.ExecuteDispatchDTO;
import com.volunteer.platform.dispatch.entity.DispatchRecommendationDO;
import com.volunteer.platform.dispatch.entity.DispatchTaskDO;
import com.volunteer.platform.dispatch.service.DispatchService;
import com.volunteer.platform.dispatch.vo.DispatchRecommendationVO;
import com.volunteer.platform.dispatch.vo.DispatchResultVO;
import com.volunteer.platform.location.client.api.LocationClient;
import com.volunteer.platform.location.client.dto.CheckinDTO;
import com.volunteer.platform.location.client.dto.NearbyUserDTO;
import com.volunteer.platform.mq.DomainEventPublisher;
import com.volunteer.platform.mq.RabbitDomainEventNames;
import com.volunteer.platform.schedule.client.api.ScheduleClient;
import com.volunteer.platform.schedule.client.dto.ScheduleAssignmentDTO;
import com.volunteer.platform.schedule.client.dto.ScheduleDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DispatchServiceImpl implements DispatchService {

    private static final String PENDING_STATUS = "PENDING";

    private static final String FINISHED_STATUS = "FINISHED";

    private static final String PARTIAL_STATUS = "PARTIAL";

    private static final String NO_CANDIDATE_STATUS = "NO_CANDIDATE";

    private static final String RECOMMENDED_STATUS = "RECOMMENDED";

    private final DispatchTaskDAO dispatchTaskDAO;

    private final DispatchRecommendationDAO dispatchRecommendationDAO;

    private final DomainEventPublisher domainEventPublisher;

    private final LocationClient locationClient;

    private final AiSchedulerClient aiSchedulerClient;

    private final ScheduleClient scheduleClient;

    public DispatchServiceImpl(DispatchTaskDAO dispatchTaskDAO,
                               DispatchRecommendationDAO dispatchRecommendationDAO,
                               DomainEventPublisher domainEventPublisher,
                               LocationClient locationClient,
                               AiSchedulerClient aiSchedulerClient) {
        this(dispatchTaskDAO, dispatchRecommendationDAO, domainEventPublisher, locationClient, aiSchedulerClient, null);
    }

    @Autowired
    public DispatchServiceImpl(DispatchTaskDAO dispatchTaskDAO,
                               DispatchRecommendationDAO dispatchRecommendationDAO,
                               DomainEventPublisher domainEventPublisher,
                               LocationClient locationClient,
                               AiSchedulerClient aiSchedulerClient,
                               ScheduleClient scheduleClient) {
        this.dispatchTaskDAO = dispatchTaskDAO;
        this.dispatchRecommendationDAO = dispatchRecommendationDAO;
        this.domainEventPublisher = domainEventPublisher;
        this.locationClient = locationClient;
        this.aiSchedulerClient = aiSchedulerClient;
        this.scheduleClient = scheduleClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DispatchResultVO execute(ExecuteDispatchDTO dto) {
        validate(dto);
        DispatchTaskDO taskDO = createTask(dto);
        dispatchTaskDAO.insert(taskDO);
        List<DispatchCandidate> candidates = recommendCandidates(dto, resolveCandidates(dto));
        int recommendCount = Math.min(dto.getRequiredCount(), candidates.size());
        for (int i = 0; i < recommendCount; i++) {
            dispatchRecommendationDAO.insert(createRecommendation(taskDO.getId(), candidates.get(i), i));
        }
        notifyRecommendedUsers(taskDO, candidates.subList(0, recommendCount));
        DispatchTaskDO statusDO = new DispatchTaskDO();
        statusDO.setId(taskDO.getId());
        statusDO.setDispatchStatus(resolveStatus(dto.getRequiredCount(), recommendCount));
        statusDO.setFinishedTime(LocalDateTime.now());
        dispatchTaskDAO.updateStatus(statusDO);
        domainEventPublisher.publish("dispatch-topic", "DISPATCH_FINISHED", "dispatch-" + taskDO.getId(),
            Map.of("dispatchTaskId", taskDO.getId(), "activityId", taskDO.getActivityId(),
                "positionId", taskDO.getPositionId(), "recommendCount", recommendCount));
        return getResult(taskDO.getId());
    }

    @Override
    public DispatchResultVO getResult(Long id) {
        DispatchTaskDO taskDO = dispatchTaskDAO.selectById(id);
        if (taskDO == null) {
            throw new BusinessException(404, "dispatch task not found");
        }
        List<DispatchRecommendationDO> recommendations = dispatchRecommendationDAO.selectByTaskId(id);
        return toResultVO(taskDO, recommendations);
    }

    @Override
    public List<DispatchResultVO> detectShortage(DetectShortageDTO dto) {
        validateShortageDTO(dto);
        ScheduleDTO scheduleDTO = querySchedule(dto.getActivityId());
        if (scheduleDTO.getAssignments() == null || scheduleDTO.getAssignments().isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> checkedInAssignmentIds = listCheckedInAssignmentIds(dto.getActivityId(), scheduleDTO.getAssignments());
        List<Long> assignedUserIds = scheduleDTO.getAssignments().stream()
            .map(ScheduleAssignmentDTO::getUserId)
            .filter(Objects::nonNull)
            .toList();
        return scheduleDTO.getAssignments().stream()
            .collect(Collectors.groupingBy(ScheduleAssignmentDTO::getPositionId, java.util.LinkedHashMap::new,
                Collectors.toList()))
            .values()
            .stream()
            .map(assignments -> createDispatchForShortage(dto, assignments, checkedInAssignmentIds, assignedUserIds))
            .filter(Objects::nonNull)
            .toList();
    }

    private void validate(ExecuteDispatchDTO dto) {
        if (dto == null || dto.getActivityId() == null || dto.getAreaId() == null || dto.getPositionId() == null) {
            throw new BusinessException(400, "dispatch target is required");
        }
        if (dto.getRequiredCount() == null || dto.getRequiredCount() <= 0) {
            throw new BusinessException(400, "required count must be positive");
        }
    }

    private void validateShortageDTO(DetectShortageDTO dto) {
        if (dto == null || dto.getActivityId() == null) {
            throw new BusinessException(400, "shortage detect target is required");
        }
    }

    private ScheduleDTO querySchedule(Long activityId) {
        if (scheduleClient == null) {
            throw new BusinessException(500, "schedule client is required");
        }
        Result<ScheduleDTO> result = scheduleClient.getActivityDetail(activityId);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BusinessException(404, "schedule plan not found");
        }
        return result.getData();
    }

    private Set<Long> listCheckedInAssignmentIds(Long activityId, List<ScheduleAssignmentDTO> assignments) {
        Set<Long> checkedInAssignmentIds = new HashSet<>();
        for (ScheduleAssignmentDTO assignment : assignments) {
            Result<List<CheckinDTO>> result = locationClient.listCheckins(activityId, assignment.getUserId());
            if (result == null || !result.isSuccess() || result.getData() == null) {
                continue;
            }
            result.getData().stream()
                .filter(checkin -> assignment.getId().equals(checkin.getAssignmentId()))
                .filter(checkin -> "NORMAL".equals(checkin.getCheckinStatus()))
                .map(CheckinDTO::getAssignmentId)
                .forEach(checkedInAssignmentIds::add);
        }
        return checkedInAssignmentIds;
    }

    private DispatchResultVO createDispatchForShortage(DetectShortageDTO dto, List<ScheduleAssignmentDTO> assignments,
                                                       Set<Long> checkedInAssignmentIds, List<Long> assignedUserIds) {
        int requiredCount = assignments.size();
        long checkedInCount = assignments.stream()
            .map(ScheduleAssignmentDTO::getId)
            .filter(checkedInAssignmentIds::contains)
            .count();
        int shortageCount = requiredCount - (int) checkedInCount;
        if (shortageCount <= 0) {
            return null;
        }
        ScheduleAssignmentDTO firstAssignment = assignments.get(0);
        ExecuteDispatchDTO executeDTO = new ExecuteDispatchDTO();
        executeDTO.setActivityId(dto.getActivityId());
        executeDTO.setAreaId(firstAssignment.getAreaId());
        executeDTO.setPositionId(firstAssignment.getPositionId());
        executeDTO.setRequiredCount(shortageCount);
        executeDTO.setReason("STAFF_SHORTAGE");
        executeDTO.setCreatedBy(dto.getCreatedBy());
        executeDTO.setLongitude(dto.getLongitude());
        executeDTO.setLatitude(dto.getLatitude());
        executeDTO.setRadiusMeter(dto.getRadiusMeter());
        executeDTO.setCandidateUserIds(listNearbyCandidateUserIds(dto, assignedUserIds));
        return execute(executeDTO);
    }

    private List<Long> listNearbyCandidateUserIds(DetectShortageDTO dto, List<Long> assignedUserIds) {
        if (dto.getLongitude() == null || dto.getLatitude() == null || dto.getRadiusMeter() == null) {
            return Collections.emptyList();
        }
        Result<List<NearbyUserDTO>> nearbyResult = locationClient.nearby(dto.getActivityId(), dto.getLongitude(),
            dto.getLatitude(), dto.getRadiusMeter());
        if (nearbyResult == null || !nearbyResult.isSuccess() || nearbyResult.getData() == null) {
            return Collections.emptyList();
        }
        Set<Long> assignedUserIdSet = new HashSet<>(assignedUserIds);
        return nearbyResult.getData().stream()
            .map(NearbyUserDTO::getUserId)
            .filter(Objects::nonNull)
            .filter(userId -> !assignedUserIdSet.contains(userId))
            .collect(Collectors.toCollection(LinkedHashSet::new))
            .stream()
            .toList();
    }

    private DispatchTaskDO createTask(ExecuteDispatchDTO dto) {
        DispatchTaskDO taskDO = new DispatchTaskDO();
        taskDO.setActivityId(dto.getActivityId());
        taskDO.setAreaId(dto.getAreaId());
        taskDO.setPositionId(dto.getPositionId());
        taskDO.setRequiredCount(dto.getRequiredCount());
        taskDO.setReason(dto.getReason());
        taskDO.setDispatchStatus(PENDING_STATUS);
        taskDO.setCreatedBy(dto.getCreatedBy());
        return taskDO;
    }

    private List<DispatchCandidate> normalizeCandidates(List<Long> candidateUserIds) {
        if (candidateUserIds == null) {
            return Collections.emptyList();
        }
        return candidateUserIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .map(userId -> new DispatchCandidate(userId, null, null))
            .toList();
    }

    private List<DispatchCandidate> resolveCandidates(ExecuteDispatchDTO dto) {
        List<DispatchCandidate> candidates = normalizeCandidates(dto.getCandidateUserIds());
        if (!candidates.isEmpty()) {
            return candidates;
        }
        if (dto.getLongitude() == null || dto.getLatitude() == null || dto.getRadiusMeter() == null) {
            return Collections.emptyList();
        }
        Result<List<NearbyUserDTO>> nearbyResult = locationClient.nearby(dto.getActivityId(), dto.getLongitude(),
            dto.getLatitude(), dto.getRadiusMeter());
        if (nearbyResult == null || !nearbyResult.isSuccess() || nearbyResult.getData() == null) {
            return Collections.emptyList();
        }
        return nearbyResult.getData().stream()
            .filter(nearbyUser -> nearbyUser.getUserId() != null)
            .map(nearbyUser -> new DispatchCandidate(nearbyUser.getUserId(), nearbyUser.getDistanceMeter(), null))
            .collect(java.util.stream.Collectors.toMap(DispatchCandidate::userId, candidate -> candidate,
                (oldCandidate, newCandidate) -> oldCandidate, java.util.LinkedHashMap::new))
            .values()
            .stream()
            .toList();
    }

    private List<DispatchCandidate> recommendCandidates(ExecuteDispatchDTO dto, List<DispatchCandidate> candidates) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        Result<AiScheduleResultDTO> result = aiSchedulerClient.recommend(toAiScheduleRequestDTO(dto, candidates));
        if (result == null || !result.isSuccess() || result.getData() == null
            || result.getData().getRecommendations() == null || result.getData().getRecommendations().isEmpty()) {
            return candidates;
        }
        Map<Long, DispatchCandidate> candidateMap = candidates.stream()
            .collect(java.util.stream.Collectors.toMap(DispatchCandidate::userId, candidate -> candidate,
                (oldCandidate, newCandidate) -> oldCandidate));
        List<DispatchCandidate> aiCandidates = result.getData().getRecommendations().stream()
            .filter(recommendation -> recommendation.getUserId() != null)
            .filter(recommendation -> candidateMap.containsKey(recommendation.getUserId()))
            .map(recommendation -> toDispatchCandidate(candidateMap.get(recommendation.getUserId()), recommendation))
            .toList();
        return aiCandidates.isEmpty() ? candidates : aiCandidates;
    }

    private AiScheduleRequestDTO toAiScheduleRequestDTO(ExecuteDispatchDTO dto, List<DispatchCandidate> candidates) {
        AiScheduleRequestDTO requestDTO = new AiScheduleRequestDTO();
        requestDTO.setActivityId(dto.getActivityId());
        requestDTO.setPositionId(dto.getPositionId());
        requestDTO.setRequiredCount(dto.getRequiredCount());
        requestDTO.setCandidates(candidates.stream().map(this::toAiCandidateDTO).toList());
        return requestDTO;
    }

    private AiCandidateDTO toAiCandidateDTO(DispatchCandidate candidate) {
        AiCandidateDTO candidateDTO = new AiCandidateDTO();
        candidateDTO.setUserId(candidate.userId());
        candidateDTO.setDistanceMeter(candidate.distanceMeter());
        candidateDTO.setAvailable(Boolean.TRUE);
        return candidateDTO;
    }

    private DispatchCandidate toDispatchCandidate(DispatchCandidate candidate, AiRecommendationDTO recommendation) {
        return new DispatchCandidate(candidate.userId(), candidate.distanceMeter(), recommendation.getMatchScore());
    }

    private DispatchRecommendationDO createRecommendation(Long taskId, DispatchCandidate candidate, int index) {
        DispatchRecommendationDO recommendationDO = new DispatchRecommendationDO();
        recommendationDO.setDispatchTaskId(taskId);
        recommendationDO.setUserId(candidate.userId());
        recommendationDO.setDistanceMeter(candidate.distanceMeter() == null ? BigDecimal.valueOf(index * 100L)
            : candidate.distanceMeter());
        recommendationDO.setMatchScore(candidate.matchScore() == null ? BigDecimal.valueOf(100L - index)
            : candidate.matchScore());
        recommendationDO.setRecommendStatus(RECOMMENDED_STATUS);
        return recommendationDO;
    }

    private void notifyRecommendedUsers(DispatchTaskDO taskDO, List<DispatchCandidate> candidates) {
        for (DispatchCandidate candidate : candidates) {
            domainEventPublisher.publish(RabbitDomainEventNames.MESSAGE_TOPIC,
                RabbitDomainEventNames.DISPATCH_NOTICE_REQUESTED,
                "dispatch-notice-" + taskDO.getId() + "-" + candidate.userId(),
                createDispatchNotice(taskDO, candidate.userId()));
        }
    }

    private Map<String, Object> createDispatchNotice(DispatchTaskDO taskDO, Long receiverId) {
        return Map.of("dispatchTaskId", taskDO.getId(),
            "activityId", taskDO.getActivityId(),
            "positionId", taskDO.getPositionId(),
            "receiverId", receiverId,
            "noticeType", "DISPATCH",
            "title", "调度通知",
            "content", "活动 " + taskDO.getActivityId() + " 岗位 " + taskDO.getPositionId() + " 需要支援，请及时处理。",
            "sendChannel", "APP");
    }

    private String resolveStatus(int requiredCount, int recommendCount) {
        if (recommendCount == 0) {
            return NO_CANDIDATE_STATUS;
        }
        if (recommendCount < requiredCount) {
            return PARTIAL_STATUS;
        }
        return FINISHED_STATUS;
    }

    private DispatchResultVO toResultVO(DispatchTaskDO taskDO, List<DispatchRecommendationDO> recommendations) {
        DispatchResultVO resultVO = new DispatchResultVO();
        resultVO.setId(taskDO.getId());
        resultVO.setActivityId(taskDO.getActivityId());
        resultVO.setAreaId(taskDO.getAreaId());
        resultVO.setPositionId(taskDO.getPositionId());
        resultVO.setRequiredCount(taskDO.getRequiredCount());
        resultVO.setReason(taskDO.getReason());
        resultVO.setDispatchStatus(taskDO.getDispatchStatus());
        resultVO.setCreatedBy(taskDO.getCreatedBy());
        resultVO.setFinishedTime(taskDO.getFinishedTime());
        resultVO.setRecommendations(recommendations.stream()
            .map(this::toRecommendationVO)
            .toList());
        return resultVO;
    }

    private DispatchRecommendationVO toRecommendationVO(DispatchRecommendationDO recommendationDO) {
        DispatchRecommendationVO recommendationVO = new DispatchRecommendationVO();
        recommendationVO.setId(recommendationDO.getId());
        recommendationVO.setUserId(recommendationDO.getUserId());
        recommendationVO.setDistanceMeter(recommendationDO.getDistanceMeter());
        recommendationVO.setMatchScore(recommendationDO.getMatchScore());
        recommendationVO.setRecommendStatus(recommendationDO.getRecommendStatus());
        return recommendationVO;
    }

    private record DispatchCandidate(Long userId, BigDecimal distanceMeter, BigDecimal matchScore) {
    }
}
