package io.jessytsiriniaina.taskmanagerapi.service;

import io.jessytsiriniaina.taskmanagerapi.entity.RefreshToken;
import io.jessytsiriniaina.taskmanagerapi.entity.User;
import io.jessytsiriniaina.taskmanagerapi.exception.InvalidRefreshTokenException;
import io.jessytsiriniaina.taskmanagerapi.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(1L, "jessy", "jessy@example.com", "password123", LocalDateTime.now(), LocalDateTime.now());
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpiration", 604800000L);
    }

    @Test
    void issue_shouldSaveHashedTokenAndReturnRawToken() {
        String rawToken = refreshTokenService.issue(user);

        assertThat(rawToken).isNotBlank();
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getTokenHash()).isEqualTo(refreshTokenService.hash(rawToken));
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(saved.getRevokedAt()).isNull();
    }

    @Test
    void hash_shouldBeDeterministic() {
        String token = "some-raw-token";

        String first = refreshTokenService.hash(token);
        String second = refreshTokenService.hash(token);

        assertThat(first).isEqualTo(second).isNotEqualTo(token);
    }

    @Test
    void hash_shouldDifferForDifferentInputs() {
        assertThat(refreshTokenService.hash("token-a"))
                .isNotEqualTo(refreshTokenService.hash("token-b"));
    }

    @Test
    void validateAndRotate_shouldRevokeOldAndIssueNew() {
        RefreshToken current = new RefreshToken("hash-1", user, LocalDateTime.now().plusDays(1));
        String rawToken = "raw-token";

        when(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(rawToken)))
                .thenReturn(Optional.of(current));

        RefreshTokenService.Grant grant = refreshTokenService.validateAndRotate(rawToken);

        assertThat(grant.user()).isEqualTo(user);
        assertThat(grant.refreshToken()).isNotBlank();
        assertThat(grant.refreshToken()).isNotEqualTo(rawToken);
        assertThat(current.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(current);
    }

    @Test
    void validateAndRotate_shouldThrowWhenTokenNotFound() {
        String rawToken = "raw-token";
        when(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(rawToken)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate(rawToken))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void validateAndRotate_shouldThrowWhenTokenRevoked() {
        RefreshToken revoked = new RefreshToken("hash-1", user, LocalDateTime.now().plusDays(1));
        revoked.setRevokedAt(LocalDateTime.now());
        String rawToken = "raw-token";

        when(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(rawToken)))
                .thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate(rawToken))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void validateAndRotate_shouldThrowWhenTokenExpired() {
        RefreshToken expired = new RefreshToken("hash-1", user, LocalDateTime.now().minusDays(1));
        String rawToken = "raw-token";

        when(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(rawToken)))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.validateAndRotate(rawToken))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void revokeAllForUser_shouldRevokeAllActiveTokens() {
        RefreshToken active1 = new RefreshToken("hash-1", user, LocalDateTime.now().plusDays(1));
        RefreshToken active2 = new RefreshToken("hash-2", user, LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(1L))
                .thenReturn(List.of(active1, active2));

        refreshTokenService.revokeAllForUser(1L);

        assertThat(active1.getRevokedAt()).isNotNull();
        assertThat(active2.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(active1);
        verify(refreshTokenRepository).save(active2);
    }

    @Test
    void revokeAllForUser_shouldDoNothingWhenNoActiveTokens() {
        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(1L))
                .thenReturn(List.of());

        refreshTokenService.revokeAllForUser(1L);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokeAllForUser_shouldOnlyRevokeActiveTokens() {
        RefreshToken active = new RefreshToken("hash-1", user, LocalDateTime.now().plusDays(1));
        RefreshToken alreadyRevoked = new RefreshToken("hash-2", user, LocalDateTime.now().plusDays(1));
        alreadyRevoked.setRevokedAt(LocalDateTime.now());

        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(1L))
                .thenReturn(List.of(active));

        refreshTokenService.revokeAllForUser(1L);

        assertThat(active.getRevokedAt()).isNotNull();
        assertThat(alreadyRevoked.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(active);
        verify(refreshTokenRepository, never()).save(alreadyRevoked);
    }
}
