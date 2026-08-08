package com.volunteer.platform.ai.service.impl;

import com.volunteer.platform.ai.client.dto.AiCandidateDTO;
import com.volunteer.platform.ai.client.dto.AiAreaRiskDTO;
import com.volunteer.platform.ai.client.dto.AiAreaRiskInputDTO;
import com.volunteer.platform.ai.client.dto.AiPredictionRequestDTO;
import com.volunteer.platform.ai.client.dto.AiPredictionResultDTO;
import com.volunteer.platform.ai.client.dto.AiPositionRiskInputDTO;
import com.volunteer.platform.ai.client.dto.AiRecommendationDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleRequestDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleResultDTO;
import com.volunteer.platform.ai.client.dto.AiShortageRiskDTO;
import com.volunteer.platform.ai.client.dto.AiVolunteerAttritionRiskDTO;
import com.volunteer.platform.ai.client.dto.AiVolunteerRiskInputDTO;
import com.volunteer.platform.ai.llm.LlmSchedulerClient;
import com.volunteer.platform.ai.service.AiSchedulerService;
import com.volunteer.platform.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class AiSchedulerServiceImpl implements AiSchedulerService {

    private static final BigDecimal DEFAULT_SCORE = BigDecimal.valueOf(60L);
    private static final BigDecimal DISTANCE_WEIGHT = BigDecimal.valueOf(0.5D);
    private static final BigDecimal HISTORY_WEIGHT = BigDecimal.valueOf(0.3D);
    private static final BigDecimal SKILL_WEIGHT = BigDecimal.valueOf(0.2D);

    private final LlmSchedulerClient llmSchedulerClient;

    public AiSchedulerServiceImpl() {
        this(null);
    }

    @Autowired
    public AiSchedulerServiceImpl(LlmSchedulerClient llmSchedulerClient) {
        this.llmSchedulerClient = llmSchedulerClient;
    }

    @Override
    public AiScheduleResultDTO recommend(AiScheduleRequestDTO requestDTO) {
        validate(requestDTO);
        if (llmSchedulerClient != null) {
            AiScheduleResultDTO llmResultDTO = llmSchedulerClient.recommend(requestDTO)
                .map(resultDTO -> normalizeLlmResult(requestDTO, resultDTO))
                .orElse(null);
            if (llmResultDTO != null && llmResultDTO.getRecommendations() != null
                && !llmResultDTO.getRecommendations().isEmpty()) {
                return llmResultDTO;
            }
        }
        return recommendByWeightedScore(requestDTO);
    }

    @Override
    public AiPredictionResultDTO predictRisks(AiPredictionRequestDTO requestDTO) {
        validatePredictionRequest(requestDTO);
        AiPredictionResultDTO resultDTO = new AiPredictionResultDTO();
        resultDTO.setActivityId(requestDTO.getActivityId());
        resultDTO.setShortageRisks(predictShortageRisks(requestDTO.getPositions()));
        resultDTO.setAttritionRisks(predictAttritionRisks(requestDTO.getVolunteers()));
        resultDTO.setAreaRisks(predictAreaRisks(requestDTO.getAreas()));
        return resultDTO;
    }

    private AiScheduleResultDTO recommendByWeightedScore(AiScheduleRequestDTO requestDTO) {
        AtomicInteger rankNo = new AtomicInteger(1);
        List<AiRecommendationDTO> recommendations = requestDTO.getCandidates().stream()
            .filter(candidate -> candidate.getUserId() != null)
            .filter(candidate -> !Boolean.FALSE.equals(candidate.getAvailable()))
            .map(candidate -> toRecommendation(candidate, calculateScore(candidate)))
            .sorted(Comparator.comparing(AiRecommendationDTO::getMatchScore).reversed()
                .thenComparing(AiRecommendationDTO::getUserId))
            .limit(requestDTO.getRequiredCount())
            .peek(recommendation -> recommendation.setRankNo(rankNo.getAndIncrement()))
            .toList();
        AiScheduleResultDTO resultDTO = new AiScheduleResultDTO();
        resultDTO.setActivityId(requestDTO.getActivityId());
        resultDTO.setPositionId(requestDTO.getPositionId());
        resultDTO.setRecommendations(recommendations);
        return resultDTO;
    }

    private AiScheduleResultDTO normalizeLlmResult(AiScheduleRequestDTO requestDTO, AiScheduleResultDTO resultDTO) {
        Map<Long, Boolean> candidateUserIdMap = requestDTO.getCandidates().stream()
            .filter(candidate -> candidate.getUserId() != null)
            .filter(candidate -> !Boolean.FALSE.equals(candidate.getAvailable()))
            .collect(Collectors.toMap(AiCandidateDTO::getUserId, candidate -> Boolean.TRUE,
                (oldValue, newValue) -> oldValue));
        AtomicInteger rankNo = new AtomicInteger(1);
        AiScheduleResultDTO normalizedDTO = new AiScheduleResultDTO();
        normalizedDTO.setActivityId(requestDTO.getActivityId());
        normalizedDTO.setPositionId(requestDTO.getPositionId());
        normalizedDTO.setRecommendations(resultDTO.getRecommendations().stream()
            .filter(recommendation -> recommendation.getUserId() != null)
            .filter(recommendation -> candidateUserIdMap.containsKey(recommendation.getUserId()))
            .limit(requestDTO.getRequiredCount())
            .map(recommendation -> normalizeRecommendation(recommendation, rankNo.getAndIncrement()))
            .filter(Objects::nonNull)
            .toList());
        return normalizedDTO;
    }

    private AiRecommendationDTO normalizeRecommendation(AiRecommendationDTO recommendationDTO, Integer rankNo) {
        if (recommendationDTO.getMatchScore() == null) {
            return null;
        }
        AiRecommendationDTO dto = new AiRecommendationDTO();
        dto.setUserId(recommendationDTO.getUserId());
        dto.setRankNo(rankNo);
        dto.setMatchScore(scoreOrDefault(recommendationDTO.getMatchScore()));
        dto.setReason(recommendationDTO.getReason());
        return dto;
    }

    private void validate(AiScheduleRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.getActivityId() == null || requestDTO.getPositionId() == null) {
            throw new BusinessException(400, "ai schedule target is required");
        }
        if (requestDTO.getRequiredCount() == null || requestDTO.getRequiredCount() <= 0) {
            throw new BusinessException(400, "required count must be positive");
        }
        if (requestDTO.getCandidates() == null || requestDTO.getCandidates().isEmpty()) {
            throw new BusinessException(400, "candidate list is required");
        }
    }

    private void validatePredictionRequest(AiPredictionRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.getActivityId() == null) {
            throw new BusinessException(400, "prediction activity is required");
        }
        if ((requestDTO.getPositions() == null || requestDTO.getPositions().isEmpty())
            && (requestDTO.getVolunteers() == null || requestDTO.getVolunteers().isEmpty())
            && (requestDTO.getAreas() == null || requestDTO.getAreas().isEmpty())) {
            throw new BusinessException(400, "prediction source data is required");
        }
    }

    private List<AiShortageRiskDTO> predictShortageRisks(List<AiPositionRiskInputDTO> positions) {
        return safeList(positions).stream()
            .filter(position -> position.getPositionId() != null)
            .map(this::toShortageRisk)
            .sorted(Comparator.comparing(AiShortageRiskDTO::getRiskScore).reversed()
                .thenComparing(AiShortageRiskDTO::getPositionId))
            .toList();
    }

    private AiShortageRiskDTO toShortageRisk(AiPositionRiskInputDTO inputDTO) {
        int requiredCount = positive(inputDTO.getRequiredCount());
        int assignedCount = positive(inputDTO.getAssignedCount());
        int checkedInCount = positive(inputDTO.getCheckedInCount());
        int shortageCount = Math.max(requiredCount - assignedCount, 0);
        BigDecimal shortageRate = rate(shortageCount, requiredCount);
        BigDecimal checkinLossRate = rate(Math.max(assignedCount - checkedInCount, 0), Math.max(assignedCount, 1));
        BigDecimal riskScore = percent(shortageRate.multiply(BigDecimal.valueOf(0.7D))
            .add(checkinLossRate.multiply(BigDecimal.valueOf(0.3D))));

        AiShortageRiskDTO riskDTO = new AiShortageRiskDTO();
        riskDTO.setPositionId(inputDTO.getPositionId());
        riskDTO.setAreaId(inputDTO.getAreaId());
        riskDTO.setPositionName(inputDTO.getPositionName());
        riskDTO.setShortageCount(shortageCount);
        riskDTO.setRiskScore(riskScore);
        riskDTO.setRiskLevel(toRiskLevel(riskScore));
        riskDTO.setReason("岗位需求 " + requiredCount + " 人，已排 " + assignedCount + " 人，已签到 " + checkedInCount
            + " 人，预测缺口 " + shortageCount + " 人。");
        return riskDTO;
    }

    private List<AiVolunteerAttritionRiskDTO> predictAttritionRisks(List<AiVolunteerRiskInputDTO> volunteers) {
        return safeList(volunteers).stream()
            .filter(volunteer -> volunteer.getUserId() != null)
            .map(this::toAttritionRisk)
            .sorted(Comparator.comparing(AiVolunteerAttritionRiskDTO::getRiskScore).reversed()
                .thenComparing(AiVolunteerAttritionRiskDTO::getUserId))
            .toList();
    }

    private AiVolunteerAttritionRiskDTO toAttritionRisk(AiVolunteerRiskInputDTO inputDTO) {
        int assignedCount = positive(inputDTO.getAssignedCount());
        int checkedInCount = positive(inputDTO.getCheckedInCount());
        int cancelledCount = positive(inputDTO.getCancelledCount());
        BigDecimal absenceRate = rate(Math.max(assignedCount - checkedInCount, 0), Math.max(assignedCount, 1));
        BigDecimal cancelScore = BigDecimal.valueOf(Math.min(cancelledCount * 15L, 100L));
        BigDecimal inactiveScore = inactiveScore(inputDTO.getLastActiveTime());
        BigDecimal riskScore = absenceRate.multiply(BigDecimal.valueOf(60L))
            .add(cancelScore.multiply(BigDecimal.valueOf(0.25D)))
            .add(inactiveScore.multiply(BigDecimal.valueOf(0.15D)))
            .setScale(2, RoundingMode.HALF_UP);

        AiVolunteerAttritionRiskDTO riskDTO = new AiVolunteerAttritionRiskDTO();
        riskDTO.setUserId(inputDTO.getUserId());
        riskDTO.setUserName(inputDTO.getUserName());
        riskDTO.setRiskScore(clampScore(riskScore));
        riskDTO.setRiskLevel(toRiskLevel(riskDTO.getRiskScore()));
        riskDTO.setReason("累计排班 " + assignedCount + " 次，签到 " + checkedInCount + " 次，取消 " + cancelledCount
            + " 次，结合活跃时间评估流失风险。");
        return riskDTO;
    }

    private List<AiAreaRiskDTO> predictAreaRisks(List<AiAreaRiskInputDTO> areas) {
        return safeList(areas).stream()
            .filter(area -> area.getAreaId() != null)
            .map(this::toAreaRisk)
            .sorted(Comparator.comparing(AiAreaRiskDTO::getRiskScore).reversed()
                .thenComparing(AiAreaRiskDTO::getAreaId))
            .toList();
    }

    private AiAreaRiskDTO toAreaRisk(AiAreaRiskInputDTO inputDTO) {
        int requiredCount = positive(inputDTO.getRequiredCount());
        int assignedCount = positive(inputDTO.getAssignedCount());
        int checkedInCount = positive(inputDTO.getCheckedInCount());
        int shortageCount = Math.max(requiredCount - assignedCount, 0);
        BigDecimal shortageRate = rate(shortageCount, requiredCount);
        BigDecimal checkinLossRate = rate(Math.max(assignedCount - checkedInCount, 0), Math.max(assignedCount, 1));
        BigDecimal riskScore = percent(shortageRate.multiply(BigDecimal.valueOf(0.55D))
            .add(checkinLossRate.multiply(BigDecimal.valueOf(0.45D))));

        AiAreaRiskDTO riskDTO = new AiAreaRiskDTO();
        riskDTO.setAreaId(inputDTO.getAreaId());
        riskDTO.setAreaName(inputDTO.getAreaName());
        riskDTO.setShortageCount(shortageCount);
        riskDTO.setRiskScore(riskScore);
        riskDTO.setRiskLevel(toRiskLevel(riskScore));
        riskDTO.setReason("区域需求 " + requiredCount + " 人，已排 " + assignedCount + " 人，已签到 " + checkedInCount
            + " 人，综合判断区域保障风险。");
        return riskDTO;
    }

    private BigDecimal calculateScore(AiCandidateDTO candidateDTO) {
        BigDecimal distanceScore = calculateDistanceScore(candidateDTO.getDistanceMeter());
        BigDecimal historyScore = scoreOrDefault(candidateDTO.getHistoricalScore());
        BigDecimal skillScore = scoreOrDefault(candidateDTO.getSkillScore());
        return distanceScore.multiply(DISTANCE_WEIGHT)
            .add(historyScore.multiply(HISTORY_WEIGHT))
            .add(skillScore.multiply(SKILL_WEIGHT))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDistanceScore(BigDecimal distanceMeter) {
        if (distanceMeter == null) {
            return DEFAULT_SCORE;
        }
        BigDecimal score = BigDecimal.valueOf(100L)
            .subtract(distanceMeter.divide(BigDecimal.valueOf(20L), 2, RoundingMode.HALF_UP));
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (score.compareTo(BigDecimal.valueOf(100L)) > 0) {
            return BigDecimal.valueOf(100L);
        }
        return score;
    }

    private BigDecimal scoreOrDefault(BigDecimal score) {
        if (score == null) {
            return DEFAULT_SCORE;
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (score.compareTo(BigDecimal.valueOf(100L)) > 0) {
            return BigDecimal.valueOf(100L);
        }
        return score;
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? List.of() : source;
    }

    private int positive(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private BigDecimal rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal rate) {
        return clampScore(rate.multiply(BigDecimal.valueOf(100L)).setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal inactiveScore(String lastActiveTime) {
        if (lastActiveTime == null || lastActiveTime.isBlank()) {
            return BigDecimal.valueOf(80L);
        }
        try {
            long inactiveDays = Math.max(ChronoUnit.DAYS.between(LocalDateTime.parse(lastActiveTime), LocalDateTime.now()), 0L);
            return BigDecimal.valueOf(Math.min(inactiveDays * 3L, 100L));
        } catch (Exception e) {
            return BigDecimal.valueOf(60L);
        }
    }

    private BigDecimal clampScore(BigDecimal score) {
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (score.compareTo(BigDecimal.valueOf(100L)) > 0) {
            return BigDecimal.valueOf(100L);
        }
        return score;
    }

    private String toRiskLevel(BigDecimal riskScore) {
        if (riskScore.compareTo(BigDecimal.valueOf(50L)) >= 0) {
            return "HIGH";
        }
        if (riskScore.compareTo(BigDecimal.valueOf(30L)) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private AiRecommendationDTO toRecommendation(AiCandidateDTO candidateDTO, BigDecimal matchScore) {
        AiRecommendationDTO recommendationDTO = new AiRecommendationDTO();
        recommendationDTO.setUserId(candidateDTO.getUserId());
        recommendationDTO.setMatchScore(matchScore);
        recommendationDTO.setReason("distance, history and skill weighted score");
        return recommendationDTO;
    }
}
