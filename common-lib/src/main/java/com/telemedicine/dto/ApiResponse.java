package com.telemedicine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int status;
    private String message;
    private T data;
    private String error;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
            .status(200)
            .message(message)
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
            .status(201)
            .message(message)
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static <T> ApiResponse<T> badRequest(String error) {
        return ApiResponse.<T>builder()
            .status(400)
            .error(error)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static <T> ApiResponse<T> notFound(String error) {
        return ApiResponse.<T>builder()
            .status(404)
            .error(error)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static <T> ApiResponse<T> unauthorized(String error) {
        return ApiResponse.<T>builder()
            .status(401)
            .error(error)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static <T> ApiResponse<T> internalError(String error) {
        return ApiResponse.<T>builder()
            .status(500)
            .error(error)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
