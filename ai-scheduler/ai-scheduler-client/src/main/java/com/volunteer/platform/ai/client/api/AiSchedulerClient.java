package com.volunteer.platform.ai.client.api;

import com.volunteer.platform.ai.client.dto.AiPredictionRequestDTO;
import com.volunteer.platform.ai.client.dto.AiPredictionResultDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleRequestDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleResultDTO;
import com.volunteer.platform.common.api.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-scheduler")
public interface AiSchedulerClient {

    @PostMapping("/ai-scheduler/recommend")
    Result<AiScheduleResultDTO> recommend(@RequestBody AiScheduleRequestDTO requestDTO);

    @PostMapping("/ai-scheduler/predictions")
    Result<AiPredictionResultDTO> predictRisks(@RequestBody AiPredictionRequestDTO requestDTO);
}
