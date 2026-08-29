package io.jessytsiriniaina.taskmanagerapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token pair returned by register, login and refresh")
public record AuthResponse(
        @Schema(description = "JWT access token. Send it as 'Authorization: Bearer <token>'.", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "JWT refresh token. Used to obtain a new token pair via /auth/refresh.", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken
) {
}