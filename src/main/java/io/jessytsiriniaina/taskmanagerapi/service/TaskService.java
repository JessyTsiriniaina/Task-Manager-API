package io.jessytsiriniaina.taskmanagerapi.service;

import io.jessytsiriniaina.taskmanagerapi.dto.TaskRequest;
import io.jessytsiriniaina.taskmanagerapi.dto.TaskResponse;
import io.jessytsiriniaina.taskmanagerapi.entity.Task;
import io.jessytsiriniaina.taskmanagerapi.entity.User;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskPriority;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskStatus;
import io.jessytsiriniaina.taskmanagerapi.exception.PaginationParamsInvalidException;
import io.jessytsiriniaina.taskmanagerapi.exception.TaskNotFoundException;
import io.jessytsiriniaina.taskmanagerapi.exception.UserNotFoundException;
import io.jessytsiriniaina.taskmanagerapi.mapper.TaskMapper;
import io.jessytsiriniaina.taskmanagerapi.repository.TaskRepository;
import io.jessytsiriniaina.taskmanagerapi.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
    }

    public List<TaskResponse> findAll() {
        return taskRepository.findAll()
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    public TaskResponse save(TaskRequest request) {
        User owner = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Task task = taskMapper.toEntity(request);
        task.setUser(owner);

        return taskMapper.toResponse(taskRepository.save(task));
    }

    public TaskResponse findById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return taskMapper.toResponse(task);
    }

    public void deleteById(Long id) {
        taskRepository.deleteById(id);
    }

    public TaskResponse update(Long id, TaskRequest request) {
        return taskRepository.findById(id)
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
        return taskRepository.findById(id)
                .map(existingTask -> {
                    existingTask.setStatus(status);
                    return taskMapper.toResponse(taskRepository.save(existingTask));
                })
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    public List<TaskResponse> findByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    public List<TaskResponse> findByPriority(TaskPriority priority) {
        return taskRepository.findByPriority(priority)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    public List<TaskResponse> findByTitleContainingIgnoreCase(String text) {
        return taskRepository.findByTitleContainingIgnoreCase(text)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    public List<TaskResponse> findByStatusAndPriority(TaskStatus status, TaskPriority priority) {
        return taskRepository.findByStatusAndPriority(status, priority)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    public Page<TaskResponse> findAll(int page, int size) {
        if(page< 0 || size < 1)
            throw new PaginationParamsInvalidException();

        Pageable pageable = PageRequest.of(page, size);

        return taskRepository.findAll(pageable)
                .map(taskMapper::toResponse);
    }

}
