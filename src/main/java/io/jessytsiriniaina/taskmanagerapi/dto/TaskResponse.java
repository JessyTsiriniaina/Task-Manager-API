package io.jessytsiriniaina.taskmanagerapi.dto;

import io.jessytsiriniaina.taskmanagerapi.enums.TaskPriority;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Task representation returned by the API")
public record TaskResponse(
        @Schema(description = "Task id", example = "1")
        Long id,

        @Schema(description = "Id of the user who owns the task", example = "1")
        Long userId,

        @Schema(description = "Task title", example = "Write Swagger documentation")
        String title,

        @Schema(description = "Task description", example = "Document every endpoint with Springdoc")
        String description,

        @Schema(description = "Task status", example = "TODO")
        TaskStatus status,

        @Schema(description = "Task priority", example = "MEDIUM")
        TaskPriority priority,

        @Schema(description = "Task due date", example = "2026-09-10T17:30:00")
        LocalDateTime dueDate,

        @Schema(description = "Creation timestamp", example = "2026-08-29T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Last update timestamp", example = "2026-08-29T10:00:00")
        LocalDateTime updatedAt
) {
}