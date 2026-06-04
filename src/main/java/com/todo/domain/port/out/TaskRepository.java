package com.todo.domain.port.out;

import com.todo.domain.model.Task;

import java.util.List;
import java.util.Optional;

/**
 * Driven port (a.k.a. "secondary port") — the contract that any
 * persistence technology must satisfy to be used by the domain.
 *
 * <p>The domain defines <em>what</em> it needs; an adapter in
 * {@code infrastructure.persistence} decides <em>how</em> (SQLite,
 * Postgres, in-memory, etc.).
 */
public interface TaskRepository {

    Task save(Task task);

    Optional<Task> findById(Long id);

    List<Task> findAll();

    void deleteById(Long id);
}
