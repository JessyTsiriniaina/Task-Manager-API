package io.jessytsiriniaina.taskmanagerapi.repository;

import io.jessytsiriniaina.taskmanagerapi.entity.BlockedToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class BlockedTokenRepositoryTest {

    @Autowired
    private BlockedTokenRepository blockedTokenRepository;

    private BlockedToken newBlockedToken(String jti) {
        return new BlockedToken(jti, LocalDateTime.now().plusDays(1));
    }

    @Test
    void existsByJtiShouldReturnTrueWhenPresent() {
        blockedTokenRepository.save(newBlockedToken("jti-abc"));

        boolean exists = blockedTokenRepository.existsByJti("jti-abc");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByJtiShouldReturnFalseWhenAbsent() {
        boolean exists = blockedTokenRepository.existsByJti("jti-unknown");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByJtiShouldReturnFalseForAnotherStoredJti() {
        blockedTokenRepository.save(newBlockedToken("jti-abc"));

        boolean exists = blockedTokenRepository.existsByJti("jti-other");

        assertThat(exists).isFalse();
    }

    @Test
    void saveWithDuplicateJtiShouldThrowDataIntegrityViolationException() {
        blockedTokenRepository.save(newBlockedToken("jti-abc"));

        assertThatThrownBy(() -> blockedTokenRepository.saveAndFlush(newBlockedToken("jti-abc")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveShouldPersistBlockedAtTimestamp() {
        BlockedToken saved = blockedTokenRepository.save(newBlockedToken("jti-abc"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getBlockedAt()).isNotNull();
    }

    @Test
    void deleteByIdShouldRemoveToken() {
        blockedTokenRepository.save(newBlockedToken("jti-abc"));

        BlockedToken saved = blockedTokenRepository.findAll().get(0);
        blockedTokenRepository.deleteById(saved.getId());

        assertThat(blockedTokenRepository.existsByJti("jti-abc")).isFalse();
        assertThat(blockedTokenRepository.count()).isZero();
    }

    @Test
    void findAllShouldReturnAllTokens() {
        blockedTokenRepository.save(newBlockedToken("jti-abc"));
        blockedTokenRepository.save(newBlockedToken("jti-def"));

        assertThat(blockedTokenRepository.findAll()).hasSize(2);
        assertThat(blockedTokenRepository.count()).isEqualTo(2);
    }
}