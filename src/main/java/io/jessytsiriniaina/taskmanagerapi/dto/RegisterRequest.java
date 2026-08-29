package io.jessytsiriniaina.taskmanagerapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Registration payload")
public record RegisterRequest(
        @Schema(description = "Unique username", example = "jessy", maxLength = 100)
        @NotBlank(message = "Username must not be blank")
        @Size(max = 100, message = "Username length must be lower than 100")
        String username,

        @Schema(description = "Valid email address", example = "jessy@example.com")
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be valid")
        String email,

        @Schema(description = "Password, at least 8 characters long", example = "strongPass123", minLength = 8)
        @NotBlank(message = "Password must not be blank")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String password
) {
}