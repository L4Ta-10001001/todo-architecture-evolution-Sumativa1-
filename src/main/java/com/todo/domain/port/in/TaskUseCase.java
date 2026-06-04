package com.todo.domain.port.in;

import com.todo.domain.model.Task;

import java.util.List;

/**
 * Driving port (a.k.a. "primary port") — the application's API as
 * expressed in Ubiquitous Language.
 *
 * <p>Anyone wanting to <em>use</em> the domain (REST controller, CLI,
 * scheduled job, gRPC adapter…) depends on this interface, never on
 * a concrete implementation.
 */
public interface TaskUseCase {

    List<Task> listAllTasks();

    Task getTaskById(Long id);

    Task createTask(String title, String description);

    Task updateTask(Long id, String title, String description);

    Task markAsCompleted(Long id);

    void deleteTask(Long id);
}
