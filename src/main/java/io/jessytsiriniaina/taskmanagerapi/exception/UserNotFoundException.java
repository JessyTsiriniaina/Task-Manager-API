package io.jessytsiriniaina.taskmanagerapi.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("There is no user with id " + id);
    }
}
