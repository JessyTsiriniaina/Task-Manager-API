package io.jessytsiriniaina.taskmanagerapi.dto;

import io.jessytsiriniaina.taskmanagerapi.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload to update only a task's status")
public record TaskStatusRequest(
        @Schema(description = "New task status", example = "IN_PROGRESS")
        @NotNull(message = "Task status is required")
        TaskStatus status
) {
}