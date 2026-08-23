package io.jessytsiriniaina.taskmanagerapi.mapper;

import io.jessytsiriniaina.taskmanagerapi.dto.TaskRequest;
import io.jessytsiriniaina.taskmanagerapi.dto.TaskResponse;
import io.jessytsiriniaina.taskmanagerapi.entity.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public Task toEntity(TaskRequest request) {
        return new Task(
                null,
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                request.dueDate(),
                null,
                null,
                null
        );
    }

    public TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getUser() == null ? null : task.getUser().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
