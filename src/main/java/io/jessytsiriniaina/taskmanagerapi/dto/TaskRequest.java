package io.jessytsiriniaina.taskmanagerapi.dto;

import io.jessytsiriniaina.taskmanagerapi.enums.TaskPriority;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "Payload to create or fully update a task")
public record TaskRequest(
        @Schema(description = "Task title", example = "Write Swagger documentation", maxLength = 100)
        @NotBlank(message = "Task title must not be blank")
        @Size(max = 100, message = "Task title length must be lower than 100")
        String title,

        @Schema(description = "Optional task description", example = "Document every endpoint with Springdoc", maxLength = 1000)
        @Size(max = 1000, message = "Task description length must be lower than 1000")
        String description,

        @Schema(description = "Task status", example = "TODO")
        @NotNull(message = "Task status is required")
        TaskStatus status,

        @Schema(description = "Task priority", example = "MEDIUM")
        @NotNull(message = "Task priority is required")
        TaskPriority priority,

        @Schema(description = "Due date, must not be in the past", example = "2026-09-10T17:30:00")
        @NotNull(message = "Task due date is required")
        @FutureOrPresent(message = "Task due date must be a future or the present date")
        LocalDateTime dueDate
) {
}