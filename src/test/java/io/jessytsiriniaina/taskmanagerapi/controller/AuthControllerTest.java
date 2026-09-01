package io.jessytsiriniaina.taskmanagerapi.controller;

import tools.jackson.databind.ObjectMapper;
import io.jessytsiriniaina.taskmanagerapi.dto.AuthResponse;
import io.jessytsiriniaina.taskmanagerapi.dto.LoginRequest;
import io.jessytsiriniaina.taskmanagerapi.dto.RefreshRequest;
import io.jessytsiriniaina.taskmanagerapi.dto.RegisterRequest;
import io.jessytsiriniaina.taskmanagerapi.entity.User;
import io.jessytsiriniaina.taskmanagerapi.exception.InvalidCredentialsException;
import io.jessytsiriniaina.taskmanagerapi.exception.InvalidRefreshTokenException;
import io.jessytsiriniaina.taskmanagerapi.exception.UserAlreadyExistsException;
import io.jessytsiriniaina.taskmanagerapi.repository.UserRepository;
import io.jessytsiriniaina.taskmanagerapi.security.JwtService;
import io.jessytsiriniaina.taskmanagerapi.service.AuthService;
import io.jessytsiriniaina.taskmanagerapi.service.BlockedTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private BlockedTokenService blockedTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(1L, "jessy", "jessy@example.com", "password123", LocalDateTime.now(), LocalDateTime.now());
        when(jwtService.extractUserId(TOKEN)).thenReturn(1L);
        when(jwtService.extractJti(TOKEN)).thenReturn("jti-1");
        when(blockedTokenService.isBlocked("jti-1")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    @Test
    void register_shouldReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest("jessy", "jessy@example.com", "password123");
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void register_whenInvalidBody_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest("", "not-an-email", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_whenUserAlreadyExists_shouldReturn409() throws Exception {
        RegisterRequest request = new RegisterRequest("jessy", "jessy@example.com", "password123");
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("Username already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_shouldReturn200() throws Exception {
        LoginRequest request = new LoginRequest("jessy@example.com", "password123");
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void login_whenInvalidBody_shouldReturn400() throws Exception {
        LoginRequest request = new LoginRequest("not-an-email", "short");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_whenInvalidCredentials_shouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest("jessy@example.com", "password123");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_shouldReturn200() throws Exception {
        RefreshRequest request = new RefreshRequest("refresh-token");
        when(authService.refresh("refresh-token"))
                .thenReturn(new AuthResponse("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    void refresh_whenBlankToken_shouldReturn400() throws Exception {
        RefreshRequest request = new RefreshRequest("");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_whenInvalidRefreshToken_shouldReturn401() throws Exception {
        RefreshRequest request = new RefreshRequest("invalid");
        when(authService.refresh("invalid"))
                .thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_shouldReturn204() throws Exception {
        doNothing().when(authService).logout(TOKEN);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNoContent());

        verify(authService).logout(TOKEN);
    }

    @Test
    void logout_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}
