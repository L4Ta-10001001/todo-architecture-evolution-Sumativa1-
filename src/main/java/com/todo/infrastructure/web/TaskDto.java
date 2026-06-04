package com.todo.infrastructure.web;

import com.todo.domain.model.Task;

/**
 * Tiny DTO used by the web adapter to translate between
 * {@link Task} (domain) and JSON (transport).
 *
 * <p>Kept inside the infrastructure layer so the domain never
 * imports Jackson or Spring.
 */
public class TaskDto {

    private final Long id;
    private final String title;
    private final String description;
    private final boolean completed;

    public TaskDto(Long id, String title, String description, boolean completed) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.completed = completed;
    }

    public static TaskDto fromDomain(Task task) {
        return new TaskDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.isCompleted()
        );
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isCompleted() { return completed; }
}
