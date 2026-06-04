package com.todo.domain.model;

/**
 * Domain exception signalling that a Task aggregate was not found.
 *
 * <p>Lives in the domain so that adapters (REST, persistence) can
 * translate it to their own protocols without coupling the domain
 * to any framework.
 */
public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long id) {
        super("Task not found: " + id);
    }
}
