package io.jessytsiriniaina.taskmanagerapi.service;

import io.jessytsiriniaina.taskmanagerapi.entity.BlockedToken;
import io.jessytsiriniaina.taskmanagerapi.repository.BlockedTokenRepository;
import io.jessytsiriniaina.taskmanagerapi.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockedTokenServiceTest {

    @Mock
    private BlockedTokenRepository blockedTokenRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private BlockedTokenService blockedTokenService;

    @Test
    void block_shouldSaveBlockedTokenWhenNotAlreadyBlocked() {
        String token = "some-jwt-token";
        String jti = "jti-123";
        LocalDateTime expiration = LocalDateTime.now().plusHours(1);

        when(jwtService.extractJti(token)).thenReturn(jti);
        when(blockedTokenRepository.existsByJti(jti)).thenReturn(false);
        when(jwtService.extractExpiration(token)).thenReturn(expiration);

        blockedTokenService.block(token);

        ArgumentCaptor<BlockedToken> captor = ArgumentCaptor.forClass(BlockedToken.class);
        verify(blockedTokenRepository).save(captor.capture());
        BlockedToken saved = captor.getValue();
        assertThat(saved.getJti()).isEqualTo(jti);
        assertThat(saved.getExpiresAt()).isEqualTo(expiration);
    }

    @Test
    void block_shouldDoNothingWhenAlreadyBlocked() {
        String token = "some-jwt-token";
        String jti = "jti-123";

        when(jwtService.extractJti(token)).thenReturn(jti);
        when(blockedTokenRepository.existsByJti(jti)).thenReturn(true);

        blockedTokenService.block(token);

        verify(blockedTokenRepository, never()).save(any());
    }

    @Test
    void isBlocked_shouldReturnTrueWhenBlocked() {
        when(blockedTokenRepository.existsByJti("jti-123")).thenReturn(true);

        boolean result = blockedTokenService.isBlocked("jti-123");

        assertThat(result).isTrue();
    }

    @Test
    void isBlocked_shouldReturnFalseWhenNotBlocked() {
        when(blockedTokenRepository.existsByJti("jti-123")).thenReturn(false);

        boolean result = blockedTokenService.isBlocked("jti-123");

        assertThat(result).isFalse();
    }
}
