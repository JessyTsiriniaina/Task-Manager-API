package io.jessytsiriniaina.taskmanagerapi.repository;

import io.jessytsiriniaina.taskmanagerapi.entity.Task;
import io.jessytsiriniaina.taskmanagerapi.entity.User;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskPriority;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskStatus;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private User newUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        return userRepository.save(user);
    }

    private Task newTask(User user, String title) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription("description");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.MEDIUM);
        task.setDueDate(LocalDateTime.now().plusDays(1));
        task.setUser(user);
        return task;
    }

    @Test
    void findByUserIdShouldReturnOnlyThatUsersTasks() {
        User alice = newUser("alice", "alice@example.com");
        User bob = newUser("bob", "bob@example.com");
        taskRepository.save(newTask(alice, "Alice task 1"));
        taskRepository.save(newTask(alice, "Alice task 2"));
        taskRepository.save(newTask(bob, "Bob task"));

        List<Task> aliceTasks = taskRepository.findByUserId(alice.getId());

        assertThat(aliceTasks).hasSize(2);
        assertThat(aliceTasks).extracting(Task::getTitle)
                .containsExactlyInAnyOrder("Alice task 1", "Alice task 2");
    }

    @Test
    void findByUserIdShouldReturnEmptyWhenUserHasNoTasks() {
        User alice = newUser("alice", "alice@example.com");
        User bob = newUser("bob", "bob@example.com");
        taskRepository.save(newTask(bob, "Bob task"));

        List<Task> aliceTasks = taskRepository.findByUserId(alice.getId());

        assertThat(aliceTasks).isEmpty();
    }

    @Test
    void findByIdAndUserIdShouldReturnTaskWhenOwnedByUser() {
        User alice = newUser("alice", "alice@example.com");
        Task task = taskRepository.save(newTask(alice, "Alice task"));

        Optional<Task> found = taskRepository.findByIdAndUserId(task.getId(), alice.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Alice task");
    }

    @Test
    void findByIdAndUserIdShouldReturnEmptyForAnotherUsersTask() {
        User alice = newUser("alice", "alice@example.com");
        User bob = newUser("bob", "bob@example.com");
        Task task = taskRepository.save(newTask(alice, "Alice task"));

        Optional<Task> found = taskRepository.findByIdAndUserId(task.getId(), bob.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void findByIdAndUserIdShouldReturnEmptyWhenTaskDoesNotExist() {
        User alice = newUser("alice", "alice@example.com");

        Optional<Task> found = taskRepository.findByIdAndUserId(999L, alice.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void deleteByIdAndUserIdShouldDeleteOnlyMatchingTask() {
        User alice = newUser("alice", "alice@example.com");
        User bob = newUser("bob", "bob@example.com");
        Task aliceTask = taskRepository.save(newTask(alice, "Alice task"));
        Task aliceTask2 = taskRepository.save(newTask(alice, "Alice task 2"));
        Task bobTask = taskRepository.save(newTask(bob, "Bob task"));

        taskRepository.deleteByIdAndUserId(aliceTask.getId(), alice.getId());

        assertThat(taskRepository.findById(aliceTask.getId())).isEmpty();
        assertThat(taskRepository.findById(aliceTask2.getId())).isPresent();
        assertThat(taskRepository.findById(bobTask.getId())).isPresent();
    }

    @Test
    void deleteByIdAndUserIdShouldNotDeleteAnotherUsersTask() {
        User alice = newUser("alice", "alice@example.com");
        User bob = newUser("bob", "bob@example.com");
        Task bobTask = taskRepository.save(newTask(bob, "Bob task"));

        taskRepository.deleteByIdAndUserId(bobTask.getId(), alice.getId());

        assertThat(taskRepository.findById(bobTask.getId())).isPresent();
    }

    @Test
    void findByUserIdWithPageableShouldReturnPagedResults() {
        User alice = newUser("alice", "alice@example.com");
        User bob = newUser("bob", "bob@example.com");
        for (int i = 1; i <= 5; i++) {
            taskRepository.save(newTask(alice, "Alice task " + i));
        }
        taskRepository.save(newTask(bob, "Bob task"));

        Page<Task> firstPage = taskRepository.findByUserId(
                alice.getId(), PageRequest.of(0, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(firstPage.getNumberOfElements()).isEqualTo(2);

        Page<Task> secondPage = taskRepository.findByUserId(
                alice.getId(), PageRequest.of(2, 2));

        assertThat(secondPage.getContent()).hasSize(1);
    }

    @Test
    void findByUserIdWithPageableShouldNotIncludeOtherUsersTasks() {
        User alice = newUser("alice", "alice@example.com");
        User bob = newUser("bob", "bob@example.com");
        taskRepository.save(newTask(alice, "Alice task"));
        for (int i = 1; i <= 3; i++) {
            taskRepository.save(newTask(bob, "Bob task " + i));
        }

        Page<Task> alicePage = taskRepository.findByUserId(
                alice.getId(), PageRequest.of(0, 10));

        assertThat(alicePage.getTotalElements()).isEqualTo(1);
        assertThat(alicePage.getContent())
                .extracting(Task::getTitle)
                .containsExactly("Alice task");
    }

    @Test
    void findAllWithSpecificationShouldFilterByStatus() {
        User alice = newUser("alice", "alice@example.com");
        taskRepository.save(newTask(alice, "Todo task"));
        Task done = newTask(alice, "Done task");
        done.setStatus(TaskStatus.DONE);
        taskRepository.save(done);

        Specification<Task> onlyDone = (root, query, cb) -> cb.equal(root.get("status"), TaskStatus.DONE);

        List<Task> result = taskRepository.findAll(onlyDone);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Done task");
    }

    @Test
    void findAllWithSpecificationShouldFilterByCombinedPredicates() {
        User alice = newUser("alice", "alice@example.com");
        taskRepository.save(newTask(alice, "Match"));
        Task high = newTask(alice, "High");
        high.setPriority(TaskPriority.HIGH);
        taskRepository.save(high);

        Specification<Task> combined = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("priority"), TaskPriority.HIGH));
            predicates.add(cb.like(root.get("title"), "%igh%"));
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        List<Task> result = taskRepository.findAll(combined);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("High");
    }

    @Test
    void findAllWithSpecificationAndPageableShouldReturnFilteredPage() {
        User alice = newUser("alice", "alice@example.com");
        for (int i = 1; i <= 4; i++) {
            taskRepository.save(newTask(alice, "Todo task " + i));
        }
        Task done = newTask(alice, "Done task");
        done.setStatus(TaskStatus.DONE);
        taskRepository.save(done);

        Specification<Task> onlyTodo = (root, query, cb) -> cb.equal(root.get("status"), TaskStatus.TODO);

        Page<Task> firstPage = taskRepository.findAll(onlyTodo, PageRequest.of(0, 3));

        assertThat(firstPage.getTotalElements()).isEqualTo(4);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(3);
        assertThat(firstPage.getContent())
                .extracting(Task::getTitle)
                .allMatch(title -> title.startsWith("Todo task"));
    }

    @Test
    void findAllWithSpecificationShouldReturnEmptyWhenNoMatch() {
        User alice = newUser("alice", "alice@example.com");
        taskRepository.save(newTask(alice, "Todo task"));

        Specification<Task> onlyDone = (root, query, cb) -> cb.equal(root.get("status"), TaskStatus.DONE);

        List<Task> result = taskRepository.findAll(onlyDone);

        assertThat(result).isEmpty();
    }

    @Test
    void countWithSpecificationShouldCountOnlyMatching() {
        User alice = newUser("alice", "alice@example.com");
        taskRepository.save(newTask(alice, "Todo task 1"));
        taskRepository.save(newTask(alice, "Todo task 2"));
        Task done = newTask(alice, "Done task");
        done.setStatus(TaskStatus.DONE);
        taskRepository.save(done);

        Specification<Task> onlyTodo = (root, query, cb) -> cb.equal(root.get("status"), TaskStatus.TODO);

        long count = taskRepository.count(onlyTodo);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void findOneWithSpecificationShouldReturnMatchingTaskWhenPresent() {
        User alice = newUser("alice", "alice@example.com");
        Task target = taskRepository.save(newTask(alice, "Target task"));
        taskRepository.save(newTask(alice, "Other task"));

        Specification<Task> byTitle = (root, query, cb) -> cb.equal(root.get("title"), "Target task");

        Optional<Task> found = taskRepository.findOne(byTitle);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(target.getId());
    }

    @Test
    void findOneWithSpecificationShouldReturnEmptyWhenNoMatch() {
        User alice = newUser("alice", "alice@example.com");
        taskRepository.save(newTask(alice, "Other task"));

        Specification<Task> byTitle = (root, query, cb) -> cb.equal(root.get("title"), "Missing");

        Optional<Task> found = taskRepository.findOne(byTitle);

        assertThat(found).isEmpty();
    }

    @Test
    void existsWithSpecificationShouldReturnTrueOrFalse() {
        User alice = newUser("alice", "alice@example.com");
        taskRepository.save(newTask(alice, "Todo task"));

        Specification<Task> onlyTodo = (root, query, cb) -> cb.equal(root.get("status"), TaskStatus.TODO);
        Specification<Task> onlyDone = (root, query, cb) -> cb.equal(root.get("status"), TaskStatus.DONE);

        assertThat(taskRepository.exists(onlyTodo)).isTrue();
        assertThat(taskRepository.exists(onlyDone)).isFalse();
    }

    @Test
    void findAllWithUserScopedSpecificationShouldIsolateUsers() {
        User alice = newUser("alice", "alice@example.com");
        User bob = newUser("bob", "bob@example.com");
        Task aliceHigh = newTask(alice, "Alice high");
        aliceHigh.setPriority(TaskPriority.HIGH);
        taskRepository.save(aliceHigh);
        taskRepository.save(newTask(alice, "Alice low"));
        taskRepository.save(newTask(bob, "Bob high"));

        Specification<Task> aliceHighOnly = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), alice.getId()));
            predicates.add(cb.equal(root.get("priority"), TaskPriority.HIGH));
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        List<Task> result = taskRepository.findAll(aliceHighOnly);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Alice high");
    }

    @Test
    void deleteByIdAndUserIdShouldBeNoOpWhenTaskDoesNotExist() {
        User alice = newUser("alice", "alice@example.com");
        taskRepository.save(newTask(alice, "Alice task"));

        taskRepository.deleteByIdAndUserId(999L, alice.getId());

        assertThat(taskRepository.count()).isEqualTo(1);
        assertThat(taskRepository.findByUserId(alice.getId())).hasSize(1);
    }

    @Test
    void findByUserIdWithPageableBeyondLastPageShouldReturnEmptyContent() {
        User alice = newUser("alice", "alice@example.com");
        for (int i = 1; i <= 2; i++) {
            taskRepository.save(newTask(alice, "Alice task " + i));
        }

        Page<Task> beyondLastPage = taskRepository.findByUserId(
                alice.getId(), PageRequest.of(5, 2));

        assertThat(beyondLastPage.getTotalElements()).isEqualTo(2);
        assertThat(beyondLastPage.getContent()).isEmpty();
    }

    @Test
    void findByUserIdWithSortShouldRespectSort() {
        User alice = newUser("alice", "alice@example.com");
        Task early = newTask(alice, "Early task");
        early.setDueDate(LocalDateTime.now().plusDays(1));
        taskRepository.save(early);
        Task late = newTask(alice, "Late task");
        late.setDueDate(LocalDateTime.now().plusDays(10));
        taskRepository.save(late);

        Page<Task> ascending = taskRepository.findByUserId(
                alice.getId(), PageRequest.of(0, 10, Sort.by("dueDate")));

        assertThat(ascending.getContent())
                .extracting(Task::getTitle)
                .containsExactly("Early task", "Late task");
    }

    @Test
    void findAllShouldReturnEmptyWhenNoTasks() {
        assertThat(taskRepository.findAll()).isEmpty();
        assertThat(taskRepository.count()).isZero();
    }

    @Test
    void existsByIdShouldReturnTrueOrFalse() {
        Task task = taskRepository.save(newTask(newUser("alice", "alice@example.com"), "Alice task"));

        assertThat(taskRepository.existsById(task.getId())).isTrue();
        assertThat(taskRepository.existsById(999L)).isFalse();
    }
}