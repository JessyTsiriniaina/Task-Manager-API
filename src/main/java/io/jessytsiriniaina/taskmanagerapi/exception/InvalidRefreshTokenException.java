package io.jessytsiriniaina.taskmanagerapi.exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
}