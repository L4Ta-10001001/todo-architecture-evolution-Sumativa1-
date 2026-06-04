package com.todo.infrastructure.web;

import com.todo.domain.port.in.TaskUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Web adapter — implements a REST binding for the driving port
 * {@link TaskUseCase}.
 *
 * <p>Notice: this class depends on the domain <em>port</em>
 * (interface), never on the persistence adapter. That's the
 * Dependency Inversion Principle in action: the adapter adapts
 * itself to the domain, not the other way around.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskUseCase taskUseCase;

    public TaskController(TaskUseCase taskUseCase) {
        this.taskUseCase = taskUseCase;
    }

    @GetMapping
    public List<TaskDto> listAllTasks() {
        return taskUseCase.listAllTasks().stream()
                .map(TaskDto::fromDomain)
                .toList();
    }

    @GetMapping("/{id}")
    public TaskDto getTask(@PathVariable Long id) {
        return TaskDto.fromDomain(taskUseCase.getTaskById(id));
    }

    @PostMapping
    public ResponseEntity<TaskDto> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskDto dto = TaskDto.fromDomain(
                taskUseCase.createTask(request.getTitle(), request.getDescription()));
        return ResponseEntity.created(URI.create("/api/tasks/" + dto.getId())).body(dto);
    }

    @PutMapping("/{id}")
    public TaskDto updateTask(@PathVariable Long id,
                              @Valid @RequestBody UpdateTaskRequest request) {
        return TaskDto.fromDomain(
                taskUseCase.updateTask(id, request.getTitle(), request.getDescription()));
    }

    @PatchMapping("/{id}/complete")
    public TaskDto completeTask(@PathVariable Long id) {
        return TaskDto.fromDomain(taskUseCase.markAsCompleted(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskUseCase.deleteTask(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
