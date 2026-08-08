package com.volunteer.platform.ai.service;

import com.volunteer.platform.ai.client.dto.AiPredictionRequestDTO;
import com.volunteer.platform.ai.client.dto.AiPredictionResultDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleRequestDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleResultDTO;

public interface AiSchedulerService {

    AiScheduleResultDTO recommend(AiScheduleRequestDTO requestDTO);

    AiPredictionResultDTO predictRisks(AiPredictionRequestDTO requestDTO);
}
