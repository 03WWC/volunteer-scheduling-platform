package com.volunteer.platform.ai.llm;

import com.volunteer.platform.ai.client.dto.AiScheduleRequestDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleResultDTO;

import java.util.Optional;

public interface LlmSchedulerClient {

    Optional<AiScheduleResultDTO> recommend(AiScheduleRequestDTO requestDTO);
}
