package io.jessytsiriniaina.taskmanagerapi.service;

import io.jessytsiriniaina.taskmanagerapi.dto.TaskRequest;
import io.jessytsiriniaina.taskmanagerapi.dto.TaskResponse;
import io.jessytsiriniaina.taskmanagerapi.entity.Task;
import io.jessytsiriniaina.taskmanagerapi.entity.User;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskPriority;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskStatus;
import io.jessytsiriniaina.taskmanagerapi.exception.PaginationParamsInvalidException;
import io.jessytsiriniaina.taskmanagerapi.exception.TaskNotFoundException;
import io.jessytsiriniaina.taskmanagerapi.mapper.TaskMapper;
import io.jessytsiriniaina.taskmanagerapi.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    public TaskService(TaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    public List<TaskResponse> findAll() {
        Long userId = getAuthenticatedUserId();
        return taskRepository.findByUserId(userId)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    public TaskResponse save(TaskRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Task task = taskMapper.toEntity(request);
        task.setUser(user);

        return taskMapper.toResponse(taskRepository.save(task));
    }

    public TaskResponse findById(Long id) {
        Long userId = getAuthenticatedUserId();
        Task task = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return taskMapper.toResponse(task);
    }

    public void deleteById(Long id) {
        Long userId = getAuthenticatedUserId();
        if (taskRepository.findByIdAndUserId(id, userId).isEmpty()) {
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteByIdAndUserId(id, userId);
    }

    public TaskResponse update(Long id, TaskRequest request) {
        Long userId = getAuthenticatedUserId();
        return taskRepository.findByIdAndUserId(id, userId)
                .map(existingTask -> {
                    existingTask.setTitle(request.title());
                    existingTask.setDescription(request.description());
                    existingTask.setStatus(request.status());
                    existingTask.setPriority(request.priority());
                    existingTask.setDueDate(request.dueDate());

                    return taskMapper.toResponse(taskRepository.save(existingTask));
                })
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    public TaskResponse updateStatus(Long id, TaskStatus status) {
        Long userId = getAuthenticatedUserId();
        return taskRepository.findByIdAndUserId(id, userId)
                .map(existingTask -> {
                    existingTask.setStatus(status);
                    return taskMapper.toResponse(taskRepository.save(existingTask));
                })
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    public List<TaskResponse> findByStatus(TaskStatus status) {
        Long userId = getAuthenticatedUserId();
        return taskRepository.findByUserIdAndStatus(userId, status)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    public List<TaskResponse> findByPriority(TaskPriority priority) {
        Long userId = getAuthenticatedUserId();
        return taskRepository.findByUserIdAndPriority(userId, priority)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    public List<TaskResponse> findByTitleContainingIgnoreCase(String text) {
        Long userId = getAuthenticatedUserId();
        return taskRepository.findByUserIdAndTitleContainingIgnoreCase(userId, text)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    public List<TaskResponse> findByStatusAndPriority(TaskStatus status, TaskPriority priority) {
        Long userId = getAuthenticatedUserId();
        return taskRepository.findByUserIdAndStatusAndPriority(userId, status, priority)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    public Page<TaskResponse> findAll(int page, int size) {
        if (page < 0 || size < 1)
            throw new PaginationParamsInvalidException();

        Long userId = getAuthenticatedUserId();
        Pageable pageable = PageRequest.of(page, size);

        return taskRepository.findByUserId(userId, pageable)
                .map(taskMapper::toResponse);
    }

    private Long getAuthenticatedUserId() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return user.getId();
    }
}
