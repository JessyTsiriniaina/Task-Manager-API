package io.jessytsiriniaina.taskmanagerapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh token payload")
public record RefreshRequest(
        @Schema(description = "Refresh token issued at login or previous refresh", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "Refresh token must not be blank")
        String refreshToken
) {
}