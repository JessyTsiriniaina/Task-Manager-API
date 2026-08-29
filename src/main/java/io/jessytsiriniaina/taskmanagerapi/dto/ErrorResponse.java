package io.jessytsiriniaina.taskmanagerapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Standard error payload returned on failed requests")
public record ErrorResponse(
        @Schema(description = "Timestamp of the error", example = "2026-08-29T10:00:00")
        LocalDateTime timestamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "HTTP status reason phrase", example = "BAD_REQUEST")
        String error,

        @Schema(description = "Human-readable error message", example = "Task title must not be blank")
        String message,

        @Schema(description = "Request URI that caused the error", example = "/api/tasks")
        String path
) {
}