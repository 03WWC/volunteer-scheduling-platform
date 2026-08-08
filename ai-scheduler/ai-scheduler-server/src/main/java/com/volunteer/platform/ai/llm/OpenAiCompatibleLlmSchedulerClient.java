package com.volunteer.platform.ai.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteer.platform.ai.client.dto.AiRecommendationDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleRequestDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleResultDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "volunteer.ai.llm", name = "enabled", havingValue = "true")
public class OpenAiCompatibleLlmSchedulerClient implements LlmSchedulerClient {

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    private final String model;

    public OpenAiCompatibleLlmSchedulerClient(@Value("${volunteer.ai.llm.base-url}") String baseUrl,
                                              @Value("${volunteer.ai.llm.api-key}") String apiKey,
                                              @Value("${volunteer.ai.llm.model}") String model,
                                              ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        this.objectMapper = objectMapper;
        this.model = model;
    }

    @Override
    public Optional<AiScheduleResultDTO> recommend(AiScheduleRequestDTO requestDTO) {
        try {
            Map<String, Object> response = restClient.post()
                .uri("/v1/chat/completions")
                .body(createRequestBody(requestDTO))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
            return parseResponse(requestDTO, response);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Map<String, Object> createRequestBody(AiScheduleRequestDTO requestDTO) {
        return Map.of("model", model,
            "temperature", 0.2,
            "messages", List.of(
                Map.of("role", "system", "content",
                    "你是志愿者调度推荐引擎。只返回 JSON，不要解释。"),
                Map.of("role", "user", "content", createPrompt(requestDTO))));
    }

    private String createPrompt(AiScheduleRequestDTO requestDTO) {
        return """
            请根据候选人的距离、历史评分、技能评分和可用状态，推荐最适合补位的人。
            返回格式：
            {"recommendations":[{"userId":101,"rankNo":1,"matchScore":98.5,"reason":"原因"}]}
            activityId=%s, positionId=%s, requiredCount=%s, candidates=%s
            """.formatted(requestDTO.getActivityId(), requestDTO.getPositionId(), requestDTO.getRequiredCount(),
            toJson(requestDTO.getCandidates()));
    }

    private Optional<AiScheduleResultDTO> parseResponse(AiScheduleRequestDTO requestDTO, Map<String, Object> response) {
        List<?> choices = (List<?>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return Optional.empty();
        }
        Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
        if (message == null || message.get("content") == null) {
            return Optional.empty();
        }
        return parseContent(requestDTO, message.get("content").toString());
    }

    private Optional<AiScheduleResultDTO> parseContent(AiScheduleRequestDTO requestDTO, String content) {
        try {
            Map<String, Object> resultMap = objectMapper.readValue(content, new TypeReference<>() {
            });
            List<?> recommendations = (List<?>) resultMap.get("recommendations");
            if (recommendations == null || recommendations.isEmpty()) {
                return Optional.empty();
            }
            AiScheduleResultDTO resultDTO = new AiScheduleResultDTO();
            resultDTO.setActivityId(requestDTO.getActivityId());
            resultDTO.setPositionId(requestDTO.getPositionId());
            resultDTO.setRecommendations(recommendations.stream()
                .map(this::toRecommendation)
                .toList());
            return Optional.of(resultDTO);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private AiRecommendationDTO toRecommendation(Object value) {
        Map<?, ?> map = (Map<?, ?>) value;
        AiRecommendationDTO dto = new AiRecommendationDTO();
        dto.setUserId(Long.valueOf(map.get("userId").toString()));
        dto.setRankNo(Integer.valueOf(map.get("rankNo").toString()));
        dto.setMatchScore(new BigDecimal(map.get("matchScore").toString()));
        dto.setReason(String.valueOf(map.get("reason")));
        return dto;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }
}
