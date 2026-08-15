package io.jessytsiriniaina.taskmanagerapi.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long id) {
        super("There is no task with id " + id);
    }
}
