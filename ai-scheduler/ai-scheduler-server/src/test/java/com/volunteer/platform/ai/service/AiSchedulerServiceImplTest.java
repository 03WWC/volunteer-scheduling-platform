package com.volunteer.platform.ai.service;

import com.volunteer.platform.ai.client.dto.AiCandidateDTO;
import com.volunteer.platform.ai.client.dto.AiAreaRiskInputDTO;
import com.volunteer.platform.ai.client.dto.AiPredictionRequestDTO;
import com.volunteer.platform.ai.client.dto.AiPredictionResultDTO;
import com.volunteer.platform.ai.client.dto.AiPositionRiskInputDTO;
import com.volunteer.platform.ai.client.dto.AiRecommendationDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleRequestDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleResultDTO;
import com.volunteer.platform.ai.client.dto.AiVolunteerRiskInputDTO;
import com.volunteer.platform.ai.llm.LlmSchedulerClient;
import com.volunteer.platform.ai.service.impl.AiSchedulerServiceImpl;
import com.volunteer.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiSchedulerServiceImplTest {

    @Test
    void recommendsAvailableCandidatesByWeightedScore() {
        AiSchedulerService service = new AiSchedulerServiceImpl();

        AiScheduleResultDTO resultDTO = service.recommend(createRequest());

        assertThat(resultDTO.getActivityId()).isEqualTo(100L);
        assertThat(resultDTO.getRecommendations()).hasSize(2);
        assertThat(resultDTO.getRecommendations()).extracting(recommendation -> recommendation.getUserId())
            .containsExactly(201L, 203L);
        assertThat(resultDTO.getRecommendations()).extracting(recommendation -> recommendation.getRankNo())
            .containsExactly(1, 2);
    }

    @Test
    void recommendThrowsBusinessExceptionWhenCandidateMissing() {
        AiSchedulerService service = new AiSchedulerServiceImpl();
        AiScheduleRequestDTO requestDTO = createRequest();
        requestDTO.setCandidates(List.of());

        assertThatThrownBy(() -> service.recommend(requestDTO))
            .isInstanceOf(BusinessException.class)
            .hasMessage("candidate list is required");
    }

    @Test
    void recommendsCandidatesWithLlmResultWhenAvailable() {
        StubLlmSchedulerClient llmSchedulerClient = new StubLlmSchedulerClient();
        llmSchedulerClient.resultDTO = createLlmResult();
        AiSchedulerService service = new AiSchedulerServiceImpl(llmSchedulerClient);

        AiScheduleResultDTO resultDTO = service.recommend(createRequest());

        assertThat(llmSchedulerClient.called).isTrue();
        assertThat(resultDTO.getRecommendations()).extracting(AiRecommendationDTO::getUserId)
            .containsExactly(203L, 201L);
        assertThat(resultDTO.getRecommendations()).extracting(AiRecommendationDTO::getReason)
            .containsExactly("llm: closer to target role", "llm: backup");
    }

    @Test
    void fallsBackToWeightedScoreWhenLlmResultMissing() {
        StubLlmSchedulerClient llmSchedulerClient = new StubLlmSchedulerClient();
        AiSchedulerService service = new AiSchedulerServiceImpl(llmSchedulerClient);

        AiScheduleResultDTO resultDTO = service.recommend(createRequest());

        assertThat(llmSchedulerClient.called).isTrue();
        assertThat(resultDTO.getRecommendations()).extracting(AiRecommendationDTO::getUserId)
            .containsExactly(201L, 203L);
    }

    @Test
    void predictsShortageAttritionAndAreaRisks() {
        AiSchedulerService service = new AiSchedulerServiceImpl();

        AiPredictionResultDTO resultDTO = service.predictRisks(createPredictionRequest());

        assertThat(resultDTO.getActivityId()).isEqualTo(100L);
        assertThat(resultDTO.getShortageRisks()).extracting(risk -> risk.getPositionId())
            .containsExactly(301L, 302L);
        assertThat(resultDTO.getShortageRisks().get(0).getRiskLevel()).isEqualTo("HIGH");
        assertThat(resultDTO.getAttritionRisks()).extracting(risk -> risk.getUserId())
            .containsExactly(401L, 402L);
        assertThat(resultDTO.getAttritionRisks().get(0).getRiskLevel()).isEqualTo("HIGH");
        assertThat(resultDTO.getAreaRisks()).extracting(risk -> risk.getAreaId())
            .containsExactly(501L, 502L);
    }

    private AiScheduleRequestDTO createRequest() {
        AiScheduleRequestDTO requestDTO = new AiScheduleRequestDTO();
        requestDTO.setActivityId(100L);
        requestDTO.setPositionId(200L);
        requestDTO.setRequiredCount(2);
        requestDTO.setCandidates(List.of(
            createCandidate(201L, "100.00", true, "90.00", "80.00"),
            createCandidate(202L, "50.00", false, "100.00", "100.00"),
            createCandidate(203L, "500.00", true, "95.00", "90.00")));
        return requestDTO;
    }

    private AiPredictionRequestDTO createPredictionRequest() {
        AiPredictionRequestDTO requestDTO = new AiPredictionRequestDTO();
        requestDTO.setActivityId(100L);
        requestDTO.setPositions(List.of(
            createPositionRiskInput(301L, 501L, "入口引导", 8, 3, 2),
            createPositionRiskInput(302L, 502L, "秩序维护", 4, 4, 4)));
        requestDTO.setVolunteers(List.of(
            createVolunteerRiskInput(401L, "张三", 5, 2, 1, "2026-07-01T09:00:00"),
            createVolunteerRiskInput(402L, "李四", 5, 5, 0, "2026-07-28T09:00:00")));
        requestDTO.setAreas(List.of(
            createAreaRiskInput(501L, "A 区", 8, 3, 2),
            createAreaRiskInput(502L, "B 区", 4, 4, 4)));
        return requestDTO;
    }

    private AiPositionRiskInputDTO createPositionRiskInput(Long positionId, Long areaId, String positionName,
                                                           Integer requiredCount, Integer assignedCount,
                                                           Integer checkedInCount) {
        AiPositionRiskInputDTO inputDTO = new AiPositionRiskInputDTO();
        inputDTO.setPositionId(positionId);
        inputDTO.setAreaId(areaId);
        inputDTO.setPositionName(positionName);
        inputDTO.setRequiredCount(requiredCount);
        inputDTO.setAssignedCount(assignedCount);
        inputDTO.setCheckedInCount(checkedInCount);
        return inputDTO;
    }

    private AiVolunteerRiskInputDTO createVolunteerRiskInput(Long userId, String userName, Integer assignedCount,
                                                             Integer checkedInCount, Integer cancelledCount,
                                                             String lastActiveTime) {
        AiVolunteerRiskInputDTO inputDTO = new AiVolunteerRiskInputDTO();
        inputDTO.setUserId(userId);
        inputDTO.setUserName(userName);
        inputDTO.setAssignedCount(assignedCount);
        inputDTO.setCheckedInCount(checkedInCount);
        inputDTO.setCancelledCount(cancelledCount);
        inputDTO.setLastActiveTime(lastActiveTime);
        return inputDTO;
    }

    private AiAreaRiskInputDTO createAreaRiskInput(Long areaId, String areaName, Integer requiredCount,
                                                   Integer assignedCount, Integer checkedInCount) {
        AiAreaRiskInputDTO inputDTO = new AiAreaRiskInputDTO();
        inputDTO.setAreaId(areaId);
        inputDTO.setAreaName(areaName);
        inputDTO.setRequiredCount(requiredCount);
        inputDTO.setAssignedCount(assignedCount);
        inputDTO.setCheckedInCount(checkedInCount);
        return inputDTO;
    }

    private AiCandidateDTO createCandidate(Long userId, String distanceMeter, Boolean available,
                                           String historicalScore, String skillScore) {
        AiCandidateDTO candidateDTO = new AiCandidateDTO();
        candidateDTO.setUserId(userId);
        candidateDTO.setDistanceMeter(new BigDecimal(distanceMeter));
        candidateDTO.setAvailable(available);
        candidateDTO.setHistoricalScore(new BigDecimal(historicalScore));
        candidateDTO.setSkillScore(new BigDecimal(skillScore));
        return candidateDTO;
    }

    private AiScheduleResultDTO createLlmResult() {
        AiScheduleResultDTO resultDTO = new AiScheduleResultDTO();
        resultDTO.setActivityId(100L);
        resultDTO.setPositionId(200L);
        resultDTO.setRecommendations(List.of(createRecommendation(203L, 1, "99.00",
                "llm: closer to target role"),
            createRecommendation(201L, 2, "95.00", "llm: backup")));
        return resultDTO;
    }

    private AiRecommendationDTO createRecommendation(Long userId, Integer rankNo, String matchScore, String reason) {
        AiRecommendationDTO recommendationDTO = new AiRecommendationDTO();
        recommendationDTO.setUserId(userId);
        recommendationDTO.setRankNo(rankNo);
        recommendationDTO.setMatchScore(new BigDecimal(matchScore));
        recommendationDTO.setReason(reason);
        return recommendationDTO;
    }

    private static class StubLlmSchedulerClient implements LlmSchedulerClient {

        private boolean called;
        private AiScheduleResultDTO resultDTO;

        @Override
        public Optional<AiScheduleResultDTO> recommend(AiScheduleRequestDTO requestDTO) {
            called = true;
            return Optional.ofNullable(resultDTO);
        }
    }
}
