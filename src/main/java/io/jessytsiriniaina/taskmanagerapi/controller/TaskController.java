package io.jessytsiriniaina.taskmanagerapi.controller;

import io.jessytsiriniaina.taskmanagerapi.entity.Task;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskPriority;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskStatus;
import io.jessytsiriniaina.taskmanagerapi.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Get all tasks")
    @GetMapping
    public ResponseEntity<List<Task>> findAll() {
        return ResponseEntity.ok(taskService.findAll());
    }

    @Operation(summary = "Create a new task")
    @PostMapping
    public ResponseEntity<Task> save(
            @Valid @RequestBody Task task
    ) {
        Task created = taskService.save(task);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @Operation(summary = "Get a task by id")
    @GetMapping("/{id}")
    public ResponseEntity<Task> findById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(taskService.findById(id));
    }

    @Operation(summary = "Delete a task by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable("id") Long id) {
        taskService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update a task by id")
    @PutMapping("/{id}")
    public ResponseEntity<Task> udpate(
            @PathVariable("id") Long id,
            @RequestBody Task updatedTask
    ) {
        return ResponseEntity.ok(taskService.update(id, updatedTask));
    }

    @GetMapping(params = {"status"})
    public ResponseEntity<List<Task>> findByStatus(
            @RequestParam("status") TaskStatus status
    ) {
        return ResponseEntity.ok(taskService.findByStatus(status));
    }

    @GetMapping(params = {"priority"})
    public ResponseEntity<List<Task>> findByPriority(
            @RequestParam("priority") TaskPriority priority
    ) {
        return ResponseEntity.ok(taskService.findByPriority(priority));
    }
}
