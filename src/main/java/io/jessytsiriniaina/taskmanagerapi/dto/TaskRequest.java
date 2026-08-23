package io.jessytsiriniaina.taskmanagerapi.dto;

import io.jessytsiriniaina.taskmanagerapi.enums.TaskPriority;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TaskRequest(
        @NotBlank(message = "Task title must not be blank")
        @Size(max = 100, message = "Task title length must be lower than 100")
        String title,

        @Size(max = 1000, message = "Task description length must be lower than 1000")
        String description,

        @NotNull(message = "Task status is required")
        TaskStatus status,

        @NotNull(message = "Task priority is required")
        TaskPriority priority,

        @NotNull(message = "Task due date is required")
        @FutureOrPresent(message = "Task due date must be a future or the present date")
        LocalDateTime dueDate,

        @NotNull(message = "Task owner is required")
        Long userId
) {
}
