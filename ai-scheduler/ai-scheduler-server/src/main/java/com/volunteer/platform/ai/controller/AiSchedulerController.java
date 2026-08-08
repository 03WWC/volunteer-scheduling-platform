package com.volunteer.platform.ai.controller;

import com.volunteer.platform.ai.client.dto.AiPredictionRequestDTO;
import com.volunteer.platform.ai.client.dto.AiPredictionResultDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleRequestDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleResultDTO;
import com.volunteer.platform.ai.service.AiSchedulerService;
import com.volunteer.platform.common.api.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai-scheduler")
public class AiSchedulerController {

    private final AiSchedulerService aiSchedulerService;

    public AiSchedulerController(AiSchedulerService aiSchedulerService) {
        this.aiSchedulerService = aiSchedulerService;
    }

    @PostMapping("/recommend")
    public Result<AiScheduleResultDTO> recommend(@RequestBody AiScheduleRequestDTO requestDTO) {
        return Result.success(aiSchedulerService.recommend(requestDTO));
    }

    @PostMapping("/predictions")
    public Result<AiPredictionResultDTO> predictRisks(@RequestBody AiPredictionRequestDTO requestDTO) {
        return Result.success(aiSchedulerService.predictRisks(requestDTO));
    }
}
