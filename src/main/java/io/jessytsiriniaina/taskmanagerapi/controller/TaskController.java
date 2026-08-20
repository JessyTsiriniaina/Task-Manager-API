package io.jessytsiriniaina.taskmanagerapi.controller;

import io.jessytsiriniaina.taskmanagerapi.dto.TaskRequest;
import io.jessytsiriniaina.taskmanagerapi.dto.TaskResponse;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskPriority;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskStatus;
import io.jessytsiriniaina.taskmanagerapi.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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
    public ResponseEntity<List<TaskResponse>> findAll() {
        return ResponseEntity.ok(taskService.findAll());
    }

    @Operation(summary = "Create a new task")
    @PostMapping
    public ResponseEntity<TaskResponse> save(
            @Valid @RequestBody TaskRequest request
    ) {
        TaskResponse created = taskService.save(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @Operation(summary = "Get a task by id")
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> findById(@PathVariable("id") Long id) {
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
    public ResponseEntity<TaskResponse> udpate(
            @PathVariable("id") Long id,
            @RequestBody TaskRequest request
    ) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    @GetMapping(params = {"status"})
    public ResponseEntity<List<TaskResponse>> findByStatus(
            @RequestParam("status") TaskStatus status
    ) {
        return ResponseEntity.ok(taskService.findByStatus(status));
    }

    @GetMapping(params = {"priority"})
    public ResponseEntity<List<TaskResponse>> findByPriority(
            @RequestParam("priority") TaskPriority priority
    ) {
        return ResponseEntity.ok(taskService.findByPriority(priority));
    }

    @GetMapping(params = {"title"})
    public ResponseEntity<List<TaskResponse>> findByTitleContainingIgnoreCase(
            @RequestParam("title") String text
    ) {
        return ResponseEntity.ok(taskService.findByTitleContainingIgnoreCase(text));
    }

    @GetMapping(params = {"status", "priority"})
    public ResponseEntity<List<TaskResponse>> findByStatusAndPriority(
            @RequestParam("status") TaskStatus status,
            @RequestParam("priority") TaskPriority priority
    ) {
        return ResponseEntity.ok(taskService.findByStatusAndPriority(status,priority));
    }


    @GetMapping(params = {"page", "size"})
    public ResponseEntity<Page<TaskResponse>> findAllPaginated(
            @RequestParam("page") int page,
            @RequestParam("size") int size
    ) {
        return ResponseEntity.ok(taskService.findAll(page, size));
    }

}
