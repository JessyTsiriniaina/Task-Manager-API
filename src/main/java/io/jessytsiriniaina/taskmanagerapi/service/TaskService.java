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
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    public TaskService(TaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
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

    public Page<TaskResponse> findAll(TaskStatus status, TaskPriority priority, String title, Integer page, Integer size) {
        Specification<Task> spec = byUserFiltered(getAuthenticatedUserId(), status, priority, title);

        if (page == null && size == null) {
            List<TaskResponse> all = taskRepository.findAll(spec)
                    .stream()
                    .map(taskMapper::toResponse)
                    .toList();
            return new PageImpl<>(all, PageRequest.of(0, Math.max(all.size(), 1)), all.size());
        }

        if (page == null || size == null) {
            throw new PaginationParamsInvalidException("Page and size must be provided together");
        }

        if (page < 0 || size < 1)
            throw new PaginationParamsInvalidException();

        return taskRepository.findAll(spec, PageRequest.of(page, size))
                .map(taskMapper::toResponse);
    }

    private Specification<Task> byUserFiltered(Long userId, TaskStatus status, TaskPriority priority, String title) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (title != null && !title.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Long getAuthenticatedUserId() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return user.getId();
    }
}
