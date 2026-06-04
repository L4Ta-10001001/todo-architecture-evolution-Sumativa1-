package com.todo.infrastructure.persistence;

import com.todo.domain.model.Task;
import com.todo.domain.port.out.TaskRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * SQLite adapter that implements the {@link TaskRepository} driven port.
 *
 * <p>All JDBC / SQL details live here. The domain layer does not know
 * that SQLite exists; swapping this adapter for a JPA or Mongo
 * implementation does not change a single line of domain code.
 */
@Component
public class SqliteTaskRepository implements TaskRepository {

    private final JdbcTemplate jdbc;

    public SqliteTaskRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        initSchema();
    }

    private void initSchema() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS tasks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "description TEXT," +
                "completed INTEGER NOT NULL DEFAULT 0)");
    }

    private static final RowMapper<Task> ROW_MAPPER = (rs, rowNum) ->
            new Task(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getInt("completed") == 1
            );

    @Override
    public Task save(Task task) {
        if (task.getId() == null) {
            return insert(task);
        }
        return update(task);
    }

    private Task insert(Task task) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO tasks(title, description, completed) VALUES (?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setInt(3, task.isCompleted() ? 1 : 0);
            return ps;
        }, keys);
        Long id = Objects.requireNonNull(keys.getKey()).longValue();
        return new Task(id, task.getTitle(), task.getDescription(), task.isCompleted());
    }

    private Task update(Task task) {
        jdbc.update(
                "UPDATE tasks SET title=?, description=?, completed=? WHERE id=?",
                task.getTitle(),
                task.getDescription(),
                task.isCompleted() ? 1 : 0,
                task.getId()
        );
        return task;
    }

    @Override
    public Optional<Task> findById(Long id) {
        List<Task> rows = jdbc.query("SELECT * FROM tasks WHERE id = ?", ROW_MAPPER, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<Task> findAll() {
        return jdbc.query("SELECT * FROM tasks", ROW_MAPPER);
    }

    @Override
    public void deleteById(Long id) {
        jdbc.update("DELETE FROM tasks WHERE id = ?", id);
    }
}
