package io.jessytsiriniaina.taskmanagerapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Login payload")
public record LoginRequest(
        @Schema(description = "Registered email address", example = "jessy@example.com")
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be valid")
        String email,

        @Schema(description = "Account password", example = "strongPass123", minLength = 8)
        @NotBlank(message = "Password must not be blank")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String password
) {
}