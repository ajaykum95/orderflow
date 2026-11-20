package com.orderflow.order.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        List<String> errors,
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(int status, String message, T data) {
        return new ApiResponse<>(true, status, message, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(int status, String message) {
        return ok(status, message, null);
    }

    public static <T> ApiResponse<T> error(int status, String message, List<String> errors) {
        return new ApiResponse<>(false, status, message, null, errors, Instant.now());
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return error(status, message, null);
    }
}
