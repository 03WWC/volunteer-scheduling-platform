package com.volunteer.platform.ai.llm;

import com.volunteer.platform.ai.client.dto.AiScheduleRequestDTO;
import com.volunteer.platform.ai.client.dto.AiScheduleResultDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "volunteer.ai.llm", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopLlmSchedulerClient implements LlmSchedulerClient {

    @Override
    public Optional<AiScheduleResultDTO> recommend(AiScheduleRequestDTO requestDTO) {
        return Optional.empty();
    }
}
