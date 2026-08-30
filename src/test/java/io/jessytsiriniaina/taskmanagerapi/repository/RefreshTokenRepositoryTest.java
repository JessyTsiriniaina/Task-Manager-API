package io.jessytsiriniaina.taskmanagerapi.repository;

import io.jessytsiriniaina.taskmanagerapi.entity.RefreshToken;
import io.jessytsiriniaina.taskmanagerapi.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@ActiveProfiles("test")
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User newUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        return userRepository.save(user);
    }

    private RefreshToken newToken(User user, String tokenHash) {
        RefreshToken token = new RefreshToken(tokenHash, user, LocalDateTime.now().plusDays(1));
        return refreshTokenRepository.save(token);
    }

    @Test
    void findByTokenHashShouldReturnTokenWhenPresent() {
        User alice = newUser("alice", "alice@example.com");
        RefreshToken token = newToken(alice, "hash-alice");

        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("hash-alice");

        assertThat(found).isPresent();
        assertThat(found.get().getTokenHash()).isEqualTo("hash-alice");
        assertThat(found.get().getUser().getUsername()).isEqualTo("alice");
    }

    @Test
    void findByTokenHashShouldReturnEmptyWhenAbsent() {
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("unknown-hash");

        assertThat(found).isEmpty();
    }

    @Test
    void findByUserIdAndRevokedAtIsNullShouldReturnOnlyActiveTokensOfThatUser() {
        User alice = newUser("alice", "alice@example.com");
        User bob = newUser("bob", "bob@example.com");
        RefreshToken active1 = newToken(alice, "hash-alice-1");
        RefreshToken active2 = newToken(alice, "hash-alice-2");
        RefreshToken revoked = newToken(alice, "hash-alice-3");
        revoked.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(revoked);
        newToken(bob, "hash-bob-1");

        List<RefreshToken> aliceActiveTokens =
                refreshTokenRepository.findByUserIdAndRevokedAtIsNull(alice.getId());

        assertThat(aliceActiveTokens).hasSize(2);
        assertThat(aliceActiveTokens)
                .extracting(RefreshToken::getTokenHash)
                .containsExactlyInAnyOrder("hash-alice-1", "hash-alice-2");
    }

    @Test
    void findByUserIdAndRevokedAtIsNullShouldReturnEmptyWhenNoActiveTokens() {
        User alice = newUser("alice", "alice@example.com");

        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(alice.getId());

        assertThat(tokens).isEmpty();
    }

    @Test
    void saveWithDuplicateTokenHashShouldThrowDataIntegrityViolationException() {
        User alice = newUser("alice", "alice@example.com");
        newToken(alice, "hash-duplicate");

        assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(
                new RefreshToken("hash-duplicate", alice, LocalDateTime.now().plusDays(1))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByTokenHashShouldBeCaseSensitive() {
        User alice = newUser("alice", "alice@example.com");
        newToken(alice, "hash-abc");

        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("HASH-ABC");

        assertThat(found).isEmpty();
    }

    @Test
    void findAllShouldReturnAllTokensAndCount() {
        User alice = newUser("alice", "alice@example.com");
        User bob = newUser("bob", "bob@example.com");
        newToken(alice, "hash-alice");
        newToken(bob, "hash-bob");

        List<RefreshToken> all = refreshTokenRepository.findAll();

        assertThat(all).hasSize(2);
        assertThat(refreshTokenRepository.count()).isEqualTo(2);
    }

    @Test
    void findAllShouldReturnEmptyWhenNoTokens() {
        assertThat(refreshTokenRepository.findAll()).isEmpty();
        assertThat(refreshTokenRepository.count()).isZero();
    }
}