package io.jessytsiriniaina.taskmanagerapi.enums;

import io.swagger.v3.oas.annotations.media.Schema;

public enum TaskPriority {
    @Schema(description = "Low importance")
    LOW,

    @Schema(description = "Medium importance")
    MEDIUM,

    @Schema(description = "High importance")
    HIGH
}