package com.volunteer.platform.common.exception;

import com.volunteer.platform.common.api.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException exception) {
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception exception) {
        LOGGER.error("Unexpected system exception", exception);
        return Result.fail(Result.SYSTEM_ERROR_CODE, Result.SYSTEM_ERROR_MESSAGE);
    }
}
