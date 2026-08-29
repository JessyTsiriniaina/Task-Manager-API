package io.jessytsiriniaina.taskmanagerapi.exception;

public class PaginationParamsInvalidException extends RuntimeException {
    public PaginationParamsInvalidException() {
        super("Page number must be greater or equal to 0 and page size must be greater that 0. ");
    }

    public PaginationParamsInvalidException(String message) {
        super(message);
    }
}
