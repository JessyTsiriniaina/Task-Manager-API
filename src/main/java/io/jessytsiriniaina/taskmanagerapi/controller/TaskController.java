package io.jessytsiriniaina.taskmanagerapi.controller;

import io.jessytsiriniaina.taskmanagerapi.dto.ErrorResponse;
import io.jessytsiriniaina.taskmanagerapi.dto.TaskRequest;
import io.jessytsiriniaina.taskmanagerapi.dto.TaskResponse;
import io.jessytsiriniaina.taskmanagerapi.dto.TaskStatusRequest;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskPriority;
import io.jessytsiriniaina.taskmanagerapi.enums.TaskStatus;
import io.jessytsiriniaina.taskmanagerapi.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tasks", description = "Operations on tasks owned by the authenticated user")
@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(
            summary = "Get tasks",
            description = "Returns the tasks of the authenticated user, optionally filtered by status, priority and title. " +
                    "Without 'page' and 'size', all matching tasks are returned in a single page. " +
                    "Provide both 'page' and 'size' to paginate; providing only one of them is rejected."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of tasks"),
            @ApiResponse(responseCode = "400", description = "Bad request. 'page' and 'size' must be provided together, " +
                    "and when provided page must be >= 0 and size must be >= 1",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired bearer token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Parameters(value = {
            @Parameter(name = "status", description = "Filter tasks by status"),
            @Parameter(name = "priority", description = "Filter tasks by priority"),
            @Parameter(name = "title", description = "Filter tasks whose title contains the given text (case-insensitive)"),
            @Parameter(name = "page", description = "Zero-based page index. Must be used together with 'size'."),
            @Parameter(name = "size", description = "Page size. Must be used together with 'page'.")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<Page<TaskResponse>> findAll(
            @RequestParam(value = "status", required = false) TaskStatus status,
            @RequestParam(value = "priority", required = false) TaskPriority priority,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        return ResponseEntity.ok(taskService.findAll(status, priority, title, page, size));
    }

    @Operation(
            summary = "Create a task",
            description = "Creates a new task owned by the authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Task created",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired bearer token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<TaskResponse> save(
            @Valid @RequestBody TaskRequest request
    ) {
        TaskResponse created = taskService.save(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @Operation(
            summary = "Get a task by id",
            description = "Returns the task with the given id if it belongs to the authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task found",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid id format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired bearer token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No task found for this id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> findById(
            @Parameter(description = "Task id", required = true, example = "1")
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(taskService.findById(id));
    }

    @Operation(
            summary = "Delete a task by id",
            description = "Deletes the task with the given id if it belongs to the authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Task deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid id format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired bearer token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No task found for this id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(
            @Parameter(description = "Task id", required = true, example = "1")
            @PathVariable("id") Long id
    ) {
        taskService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Update a task by id",
            description = "Replaces all fields of the task with the given id if it belongs to the authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task updated",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body or id format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired bearer token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No task found for this id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(
            @Parameter(description = "Task id", required = true, example = "1")
            @PathVariable("id") Long id,
            @RequestBody TaskRequest request
    ) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    @Operation(
            summary = "Update a task status by id",
            description = "Updates only the status of the task with the given id if it belongs to the authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task status updated",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body or id format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired bearer token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No task found for this id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}")
    public ResponseEntity<TaskResponse> updateStatus(
            @Parameter(description = "Task id", required = true, example = "1")
            @PathVariable("id") Long id,
            @Valid @RequestBody TaskStatusRequest request
    ) {
        return ResponseEntity.ok(taskService.updateStatus(id, request.status()));
    }

}