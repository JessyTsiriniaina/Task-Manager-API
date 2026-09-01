package io.jessytsiriniaina.taskmanagerapi.service;

import io.jessytsiriniaina.taskmanagerapi.dto.AuthResponse;
import io.jessytsiriniaina.taskmanagerapi.dto.LoginRequest;
import io.jessytsiriniaina.taskmanagerapi.dto.RegisterRequest;
import io.jessytsiriniaina.taskmanagerapi.entity.User;
import io.jessytsiriniaina.taskmanagerapi.exception.InvalidCredentialsException;
import io.jessytsiriniaina.taskmanagerapi.exception.UserAlreadyExistsException;
import io.jessytsiriniaina.taskmanagerapi.repository.UserRepository;
import io.jessytsiriniaina.taskmanagerapi.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private BlockedTokenService blockedTokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private User user() {
        User u = new User(1L, "jessy", "jessy@example.com", "encoded-password", LocalDateTime.now(), LocalDateTime.now());
        return u;
    }

    @Test
    void register_shouldCreateUserAndReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest("jessy", "jessy@example.com", "password123");

        when(userRepository.findByUsername("jessy")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("jessy@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(jwtService.generateToken(org.mockito.ArgumentMatchers.any())).thenReturn("access-token");
        when(refreshTokenService.issue(org.mockito.ArgumentMatchers.any(User.class))).thenReturn("refresh-token");

        AuthResponse result = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("jessy");
        assertThat(captor.getValue().getEmail()).isEqualTo("jessy@example.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void register_shouldThrowWhenUsernameExists() {
        RegisterRequest request = new RegisterRequest("jessy", "jessy@example.com", "password123");
        when(userRepository.findByUsername("jessy")).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void register_shouldThrowWhenEmailExists() {
        RegisterRequest request = new RegisterRequest("jessy", "jessy@example.com", "password123");
        when(userRepository.findByUsername("jessy")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("jessy@example.com")).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void login_shouldReturnAuthResponseWhenCredentialsValid() {
        LoginRequest request = new LoginRequest("jessy@example.com", "password123");

        when(userRepository.findByEmail("jessy@example.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken(org.mockito.ArgumentMatchers.anyLong())).thenReturn("access-token");
        when(refreshTokenService.issue(org.mockito.ArgumentMatchers.any(User.class))).thenReturn("refresh-token");

        AuthResponse result = authService.login(request);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_shouldThrowWhenEmailNotFound() {
        LoginRequest request = new LoginRequest("jessy@example.com", "password123");
        when(userRepository.findByEmail("jessy@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_shouldThrowWhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest("jessy@example.com", "wrong-password");

        when(userRepository.findByEmail("jessy@example.com")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_shouldGenerateNewAccessTokenAndReturnPair() {
        RefreshTokenService.Grant grant = new RefreshTokenService.Grant(user(), "new-refresh-token");
        when(refreshTokenService.validateAndRotate("old-refresh-token")).thenReturn(grant);
        when(jwtService.generateToken(1L)).thenReturn("new-access-token");

        AuthResponse result = authService.refresh("old-refresh-token");

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void refresh_shouldPropagateInvalidRefreshTokenException() {
        when(refreshTokenService.validateAndRotate("invalid"))
                .thenThrow(new InvalidCredentialsException());

        assertThatThrownBy(() -> authService.refresh("invalid"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void logout_shouldBlockTokenAndRevokeAllRefreshTokens() {
        when(jwtService.extractUserId("access-token")).thenReturn(1L);

        authService.logout("access-token");

        verify(jwtService).extractUserId("access-token");
        verify(blockedTokenService).block("access-token");
        verify(refreshTokenService).revokeAllForUser(1L);
    }
}
