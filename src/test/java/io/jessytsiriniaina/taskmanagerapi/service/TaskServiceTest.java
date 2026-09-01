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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    private User user;
    private Task task;
    private TaskRequest request;
    private TaskResponse response;

    @BeforeEach
    void setUp() {
        user = new User(1L, "jessy", "jessy@example.com", "password123", LocalDateTime.now(), LocalDateTime.now());

        task = new Task(
                10L,
                "Title",
                "Description",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                LocalDateTime.now().plusDays(1),
                user,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        request = new TaskRequest(
                "Title",
                "Description",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                LocalDateTime.now().plusDays(1)
        );

        response = new TaskResponse(
                10L,
                1L,
                "Title",
                "Description",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void save_shouldPersistTaskWithAuthenticatedUser() {
        when(taskMapper.toEntity(request)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(response);

        TaskResponse result = taskService.save(request);

        assertThat(result).isEqualTo(response);
        assertThat(task.getUser()).isEqualTo(user);
        verify(taskMapper).toEntity(request);
        verify(taskRepository).save(task);
        verify(taskMapper).toResponse(task);
    }

    @Test
    void findById_shouldReturnTaskWhenFound() {
        when(taskRepository.findByIdAndUserId(10L, user.getId())).thenReturn(Optional.of(task));
        when(taskMapper.toResponse(task)).thenReturn(response);

        TaskResponse result = taskService.findById(10L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void findById_shouldThrowWhenTaskNotFound() {
        when(taskRepository.findByIdAndUserId(10L, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(10L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void deleteById_shouldDeleteWhenTaskExists() {
        when(taskRepository.findByIdAndUserId(10L, user.getId())).thenReturn(Optional.of(task));

        taskService.deleteById(10L);

        verify(taskRepository).deleteByIdAndUserId(10L, user.getId());
    }

    @Test
    void deleteById_shouldThrowWhenTaskNotFound() {
        when(taskRepository.findByIdAndUserId(10L, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteById(10L))
                .isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, never()).deleteByIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void update_shouldUpdateAllFieldsWhenTaskExists() {
        when(taskRepository.findByIdAndUserId(10L, user.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(response);

        TaskResponse result = taskService.update(10L, request);

        assertThat(result).isEqualTo(response);
        assertThat(task.getTitle()).isEqualTo("Title");
        assertThat(task.getDescription()).isEqualTo("Description");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(task.getDueDate()).isEqualTo(request.dueDate());
        verify(taskRepository).save(task);
    }

    @Test
    void update_shouldThrowWhenTaskNotFound() {
        when(taskRepository.findByIdAndUserId(10L, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(10L, request))
                .isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateStatus_shouldUpdateOnlyStatusWhenTaskExists() {
        when(taskRepository.findByIdAndUserId(10L, user.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(response);

        TaskResponse result = taskService.updateStatus(10L, TaskStatus.DONE);

        assertThat(result).isEqualTo(response);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void updateStatus_shouldThrowWhenTaskNotFound() {
        when(taskRepository.findByIdAndUserId(10L, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateStatus(10L, TaskStatus.DONE))
                .isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void findAll_withoutPagination_shouldReturnAllInSinglePage() {
        when(taskRepository.findAll(any(Specification.class))).thenReturn(List.of(task));
        when(taskMapper.toResponse(task)).thenReturn(response);

        Page<TaskResponse> result = taskService.findAll(null, null, null, null, null);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(taskRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void findAll_withoutPagination_shouldReturnEmptyPageWhenNoResults() {
        when(taskRepository.findAll(any(Specification.class))).thenReturn(List.of());

        Page<TaskResponse> result = taskService.findAll(null, null, null, null, null);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void findAll_withPagination_shouldReturnPagedResults() {
        Page<Task> taskPage = new PageImpl<>(List.of(task));
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(taskPage);
        when(taskMapper.toResponse(task)).thenReturn(response);

        Page<TaskResponse> result = taskService.findAll(null, null, null, 0, 10);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void findAll_withOnlyPage_shouldThrow() {
        assertThatThrownBy(() -> taskService.findAll(null, null, null, 0, null))
                .isInstanceOf(PaginationParamsInvalidException.class)
                .hasMessage("Page and size must be provided together");
    }

    @Test
    void findAll_withOnlySize_shouldThrow() {
        assertThatThrownBy(() -> taskService.findAll(null, null, null, null, 10))
                .isInstanceOf(PaginationParamsInvalidException.class)
                .hasMessage("Page and size must be provided together");
    }

    @Test
    void findAll_withNegativePage_shouldThrow() {
        assertThatThrownBy(() -> taskService.findAll(null, null, null, -1, 10))
                .isInstanceOf(PaginationParamsInvalidException.class);
    }

    @Test
    void findAll_withZeroSize_shouldThrow() {
        assertThatThrownBy(() -> taskService.findAll(null, null, null, 0, 0))
                .isInstanceOf(PaginationParamsInvalidException.class);
    }

    @Test
    void findAll_shouldFilterByStatusPriorityAndTitle() {
        when(taskRepository.findAll(any(Specification.class))).thenReturn(List.of(task));
        when(taskMapper.toResponse(task)).thenReturn(response);

        Page<TaskResponse> result = taskService.findAll(TaskStatus.TODO, TaskPriority.MEDIUM, "Title", null, null);

        assertThat(result.getContent()).containsExactly(response);
        ArgumentCaptor<Specification<Task>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(taskRepository).findAll(captor.capture());
        assertThat(captor.getValue()).isNotNull();
    }
}
