package com.volunteer.platform.common.exception;

import com.volunteer.platform.common.api.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void handlesBusinessExceptionWithExceptionCodeAndMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Result<Object> result = handler.handleBusinessException(new BusinessException(409, "activity exists"));

        assertThat(result.getCode()).isEqualTo(409);
        assertThat(result.getMessage()).isEqualTo("activity exists");
        assertThat(result.getData()).isNull();
    }

    @Test
    void handlesUnexpectedExceptionWithGenericMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Result<Object> result = handler.handleException(new IllegalStateException("database offline"));

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("system error");
        assertThat(result.getData()).isNull();
    }
}
