package com.volunteer.platform.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void successWrapsDataWithStandardCodeAndMessage() {
        Result<String> result = Result.success("created");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).isEqualTo("created");
    }

    @Test
    void failWrapsBusinessErrorWithoutData() {
        Result<Object> result = Result.fail(400, "invalid request");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("invalid request");
        assertThat(result.getData()).isNull();
    }
}
