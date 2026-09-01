package io.jessytsiriniaina.taskmanagerapi.controller;

import tools.jackson.databind.ObjectMapper;
import io.jessytsiriniaina.taskmanagerapi.dto.TaskRequest;
import io.jessytsiriniaina.taskmanagerapi.dto.TaskResponse;
import io.jessytsiriniaina.taskmanagerapi.dto.TaskStatusRequest;
import io.jessytsiriniaina.taskmanagerapi.entity.User;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskPriority;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskStatus;
import io.jessytsiriniaina.taskmanagerapi.exception.PaginationParamsInvalidException;
import io.jessytsiriniaina.taskmanagerapi.exception.TaskNotFoundException;
import io.jessytsiriniaina.taskmanagerapi.repository.UserRepository;
import io.jessytsiriniaina.taskmanagerapi.security.JwtService;
import io.jessytsiriniaina.taskmanagerapi.service.BlockedTokenService;
import io.jessytsiriniaina.taskmanagerapi.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private BlockedTokenService blockedTokenService;

    private User user;
    private TaskResponse response;
    private TaskRequest request;

    @BeforeEach
    void setUp() {
        user = new User(1L, "jessy", "jessy@example.com", "password123", LocalDateTime.now(), LocalDateTime.now());
        response = new TaskResponse(
                10L, 1L, "Title", "Description",
                TaskStatus.TODO, TaskPriority.MEDIUM,
                LocalDateTime.now().plusDays(1), LocalDateTime.now(), LocalDateTime.now()
        );
        request = new TaskRequest(
                "Title", "Description",
                TaskStatus.TODO, TaskPriority.MEDIUM,
                LocalDateTime.now().plusDays(1)
        );

        when(jwtService.extractUserId(TOKEN)).thenReturn(1L);
        when(jwtService.extractJti(TOKEN)).thenReturn("jti-1");
        when(blockedTokenService.isBlocked("jti-1")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    @Test
    void findAll_shouldReturnTasks() throws Exception {
        when(taskService.findAll(isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].title").value("Title"));
    }

    @Test
    void findAll_withPagination_shouldReturnTasks() throws Exception {
        when(taskService.findAll(isNull(), isNull(), isNull(), eq(0), eq(10)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/tasks")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void findAll_withFilter_shouldReturnTasks() throws Exception {
        when(taskService.findAll(eq(TaskStatus.TODO), eq(TaskPriority.HIGH), eq("Ti"), isNull(), isNull()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/tasks")
                        .param("status", "TODO")
                        .param("priority", "HIGH")
                        .param("title", "Ti")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void findAll_whenPaginationInvalid_shouldReturn400() throws Exception {
        when(taskService.findAll(isNull(), isNull(), isNull(), eq(1), isNull()))
                .thenThrow(new PaginationParamsInvalidException("Page and size must be provided together"));

        mockMvc.perform(get("/api/tasks")
                        .param("page", "1")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_shouldReturnTask() throws Exception {
        when(taskService.findById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/tasks/10")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void findById_whenNotFound_shouldReturn404() throws Exception {
        when(taskService.findById(10L)).thenThrow(new TaskNotFoundException(10L));

        mockMvc.perform(get("/api/tasks/10")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void save_shouldReturn201() throws Exception {
        when(taskService.save(any(TaskRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void save_whenBlankTitle_shouldReturn400() throws Exception {
        TaskRequest invalid = new TaskRequest("", "desc", TaskStatus.TODO, TaskPriority.MEDIUM, LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void save_whenNullStatus_shouldReturn400() throws Exception {
        TaskRequest invalid = new TaskRequest("Title", "desc", null, TaskPriority.MEDIUM, LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void save_whenPastDueDate_shouldReturn400() throws Exception {
        TaskRequest invalid = new TaskRequest("Title", "desc", TaskStatus.TODO, TaskPriority.MEDIUM, LocalDateTime.now().minusDays(1));

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldReturn200() throws Exception {
        when(taskService.update(eq(10L), any(TaskRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/tasks/10")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void update_whenNotFound_shouldReturn404() throws Exception {
        when(taskService.update(eq(10L), any(TaskRequest.class))).thenThrow(new TaskNotFoundException(10L));

        mockMvc.perform(put("/api/tasks/10")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_shouldReturn200() throws Exception {
        when(taskService.updateStatus(eq(10L), eq(TaskStatus.DONE))).thenReturn(response);

        mockMvc.perform(patch("/api/tasks/10")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskStatusRequest(TaskStatus.DONE))))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_whenNullStatus_shouldReturn400() throws Exception {
        mockMvc.perform(patch("/api/tasks/10")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_whenNotFound_shouldReturn404() throws Exception {
        when(taskService.updateStatus(eq(10L), eq(TaskStatus.DONE))).thenThrow(new TaskNotFoundException(10L));

        mockMvc.perform(patch("/api/tasks/10")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TaskStatusRequest(TaskStatus.DONE))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteById_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/tasks/10")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteById_whenNotFound_shouldReturn404() throws Exception {
        doThrow(new TaskNotFoundException(10L)).when(taskService).deleteById(10L);

        mockMvc.perform(delete("/api/tasks/10")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void save_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
