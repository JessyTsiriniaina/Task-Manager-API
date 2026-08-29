package io.jessytsiriniaina.taskmanagerapi.enums;

import io.swagger.v3.oas.annotations.media.Schema;

public enum TaskStatus {
    @Schema(description = "Task not started yet")
    TODO,

    @Schema(description = "Task is being worked on")
    IN_PROGRESS,

    @Schema(description = "Task is completed")
    DONE
}