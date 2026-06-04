package com.todo.domain.service;

import com.todo.domain.model.Task;
import com.todo.domain.model.TaskNotFoundException;
import com.todo.domain.port.in.TaskUseCase;
import com.todo.domain.port.out.TaskRepository;

import java.util.List;

/**
 * Domain service that implements {@link TaskUseCase}.
 *
 * <p>This is where business rules live. Notice it depends only on
 * domain ports (interfaces) — not on Spring, not on JDBC, not on
 * HTTP. That is the whole point of Hexagonal Architecture: the
 * domain is the most stable part of the system.
 */
public class TaskDomainService implements TaskUseCase {

    private final TaskRepository taskRepository;

    public TaskDomainService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public List<Task> listAllTasks() {
        return taskRepository.findAll();
    }

    @Override
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Override
    public Task createTask(String title, String description) {
        Task newTask = Task.createNew(title, description);
        return taskRepository.save(newTask);
    }

    @Override
    public Task updateTask(Long id, String title, String description) {
        Task existing = getTaskById(id);
        existing.updateDetails(title, description);
        return taskRepository.save(existing);
    }

    @Override
    public Task markAsCompleted(Long id) {
        Task existing = getTaskById(id);
        existing.complete();
        return taskRepository.save(existing);
    }

    @Override
    public void deleteTask(Long id) {
        getTaskById(id);
        taskRepository.deleteById(id);
    }
}
