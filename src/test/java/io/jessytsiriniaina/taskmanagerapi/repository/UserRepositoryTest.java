package io.jessytsiriniaina.taskmanagerapi.repository;

import io.jessytsiriniaina.taskmanagerapi.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User newUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        return user;
    }

    @Test
    void saveShouldPersistUserWithIdAndTimestamps() {
        User saved = userRepository.save(newUser("alice", "alice@example.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByUsernameShouldReturnUserWhenPresent() {
        userRepository.save(newUser("alice", "alice@example.com"));

        Optional<User> found = userRepository.findByUsername("alice");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void findByUsernameShouldReturnEmptyWhenAbsent() {
        Optional<User> found = userRepository.findByUsername("nobody");

        assertThat(found).isEmpty();
    }

    @Test
    void findByEmailShouldReturnUserWhenPresent() {
        userRepository.save(newUser("alice", "alice@example.com"));

        Optional<User> found = userRepository.findByEmail("alice@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("alice");
    }

    @Test
    void findByEmailShouldReturnEmptyWhenAbsent() {
        Optional<User> found = userRepository.findByEmail("unknown@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void findByUsernameShouldOnlyReturnMatchingUser() {
        userRepository.save(newUser("alice", "alice@example.com"));
        userRepository.save(newUser("bob", "bob@example.com"));

        Optional<User> found = userRepository.findByUsername("bob");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    void findByIdShouldReturnUserWhenPresent() {
        User saved = userRepository.save(newUser("alice", "alice@example.com"));

        Optional<User> found = userRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("alice");
    }

    @Test
    void findByIdShouldReturnEmptyWhenAbsent() {
        Optional<User> found = userRepository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void saveWithDuplicateUsernameShouldThrowDataIntegrityViolationException() {
        userRepository.save(newUser("alice", "alice@example.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("alice", "other@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveWithDuplicateEmailShouldThrowDataIntegrityViolationException() {
        userRepository.save(newUser("alice", "alice@example.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("bob", "alice@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findAllShouldReturnEmptyWhenNoUsers() {
        List<User> users = userRepository.findAll();

        assertThat(users).isEmpty();
    }

    @Test
    void findAllShouldReturnAllUsersAndCount() {
        userRepository.save(newUser("alice", "alice@example.com"));
        userRepository.save(newUser("bob", "bob@example.com"));

        List<User> users = userRepository.findAll();

        assertThat(users).hasSize(2);
        assertThat(userRepository.count()).isEqualTo(2);
    }

    @Test
    void findByUsernameShouldBeCaseSensitive() {
        userRepository.save(newUser("alice", "alice@example.com"));

        Optional<User> found = userRepository.findByUsername("Alice");

        assertThat(found).isEmpty();
    }

    @Test
    void findByEmailShouldBeCaseSensitive() {
        userRepository.save(newUser("alice", "alice@example.com"));

        Optional<User> found = userRepository.findByEmail("ALICE@EXAMPLE.COM");

        assertThat(found).isEmpty();
    }
}