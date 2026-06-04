package com.todo.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound JSON payload. Validated with Jakarta Bean Validation
 * (framework-side concern, lives in infrastructure).
 */
public class CreateTaskRequest {

    @NotBlank(message = "title is required")
    private String title;

    private String description;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
