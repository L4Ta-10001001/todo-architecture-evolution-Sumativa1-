package com.todo.domain.model;

import java.util.Objects;

/**
 * Task — pure domain entity (Aggregate Root in DDD terms).
 *
 * <p>This class has <strong>zero</strong> framework dependencies: no
 * Spring, no JPA, no Jackson, no annotations beyond the JDK.
 * It carries identity, state and behaviour — not just data.
 *
 * <p>Behavioural methods express the Ubiquitous Language:
 * {@link #complete()}, {@link #updateDetails(String, String)},
 * {@link #isCompleted()}.
 */
public class Task {

    private final Long id;
    private String title;
    private String description;
    private boolean completed;

    public Task(Long id, String title, String description, boolean completed) {
        this.id = id;
        this.title = Objects.requireNonNull(title, "title must not be null").trim();
        this.description = description;
        this.completed = completed;
        validateTitle();
    }

    /**
     * Factory used by the domain service to create a brand-new Task
     * whose identity will be assigned by the persistence adapter.
     */
    public static Task createNew(String title, String description) {
        return new Task(null, title, description, false);
    }

    public void complete() {
        this.completed = true;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void updateDetails(String newTitle, String newDescription) {
        this.title = Objects.requireNonNull(newTitle, "title must not be null").trim();
        this.description = newDescription;
        validateTitle();
    }

    public void renameTo(String newTitle) {
        this.title = Objects.requireNonNull(newTitle, "title must not be null").trim();
        validateTitle();
    }

    private void validateTitle() {
        if (title.isEmpty()) {
            throw new IllegalArgumentException("title must not be blank");
        }
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
}
