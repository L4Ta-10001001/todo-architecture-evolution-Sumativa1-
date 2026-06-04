package com.todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;

/**
 * ANTI-PATTERN: God class.
 *
 * This single class is responsible for:
 *   - bootstrapping the Spring application
 *   - exposing every REST endpoint
 *   - opening SQLite connections inline
 *   - running SQL DDL and DML queries
 *   - validating input
 *   - converting between row maps and "DTO-like" Maps
 *   - deciding HTTP status codes
 *   - logging (sort of, via System.out)
 *
 * There is no service, no repository, no DTO, no entity, no interface.
 * Everything lives here. Maintenance will be a nightmare.
 */
@SpringBootApplication
@RestController
public class Main {

    // ANTI-PATTERN: hardcoded connection string, duplicated with application.properties.
    private static final String DB = "jdbc:sqlite:todo.db";

    public static void main(String[] args) throws Exception {
        // ANTI-PATTERN: DDL initialization inside the main method.
        try (Connection c = DriverManager.getConnection(DB);
             Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS tasks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT NOT NULL," +
                    "description TEXT," +
                    "completed INTEGER DEFAULT 0)");
        }
        SpringApplication.run(Main.class, args);
    }

    // -------- Helpers (which are themselves anti-patterns) --------

    // ANTI-PATTERN: business/data logic embedded in the controller layer.
    private Connection getConn() throws SQLException {
        return DriverManager.getConnection(DB);
    }

    // ANTI-PATTERN: SQL knowledge leaks into the controller, no repository abstraction.
    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> m = new HashMap<>();
        m.put("id", rs.getLong("id"));
        m.put("title", rs.getString("title"));
        m.put("description", rs.getString("description"));
        m.put("completed", rs.getInt("completed") == 1);
        return m;
    }

    // ANTI-PATTERN: validation lives next to HTTP routing.
    private String validate(Map<String, Object> data) {
        Object t = data.get("title");
        if (t == null || t.toString().trim().isEmpty()) {
            return "title is required";
        }
        return null;
    }

    // -------- Endpoints (all in one class) --------

    // ANTI-PATTERN: routing, validation, persistence and serialization are tangled.
    @GetMapping("/api/tasks")
    public Object list() {
        try (Connection c = getConn();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM tasks")) {
            List<Map<String, Object>> out = new ArrayList<>();
            while (rs.next()) out.add(rowToMap(rs));
            return out;
        } catch (Exception e) {
            // ANTI-PATTERN: catching generic Exception, swallowing stacktrace, returning raw text.
            return Map.of("error", e.getMessage());
        }
    }

    // ANTI-PATTERN: generic parameter name `data`, no DTO.
    @PostMapping("/api/tasks")
    public Object create(@RequestBody Map<String, Object> data) {
        String err = validate(data);
        if (err != null) return ResponseEntity.badRequest().body(Map.of("error", err));

        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO tasks(title, description, completed) VALUES (?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, (String) data.get("title"));
            ps.setString(2, (String) data.getOrDefault("description", ""));
            ps.setInt(3, 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return getById(keys.getLong(1));
            }
            return Map.of("error", "unknown");
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    // ANTI-PATTERN: parameter name `x`, primitive long exposed directly.
    @GetMapping("/api/tasks/{x}")
    public Object get(@PathVariable long x) {
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM tasks WHERE id = ?")) {
            ps.setLong(1, x);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return ResponseEntity.notFound().build();
                return rowToMap(rs);
            }
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    // ANTI-PATTERN: update logic mixed with HTTP layer.
    @PutMapping("/api/tasks/{x}")
    public Object update(@PathVariable long x, @RequestBody Map<String, Object> data) {
        try (Connection c = getConn()) {
            // check existence
            try (PreparedStatement chk = c.prepareStatement("SELECT id FROM tasks WHERE id=?")) {
                chk.setLong(1, x);
                try (ResultSet rs = chk.executeQuery()) {
                    if (!rs.next()) return ResponseEntity.notFound().build();
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE tasks SET title=?, description=?, completed=? WHERE id=?")) {
                ps.setString(1, (String) data.get("title"));
                ps.setString(2, (String) data.getOrDefault("description", ""));
                ps.setInt(3, data.get("completed") != null && (boolean) data.get("completed") ? 1 : 0);
                ps.setLong(4, x);
                ps.executeUpdate();
            }
            return getById(x);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    // ANTI-PATTERN: PATCH (mark as completed) implemented as another god-method.
    @PatchMapping("/api/tasks/{x}/complete")
    public Object complete(@PathVariable long x) {
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE tasks SET completed=1 WHERE id=?")) {
            ps.setLong(1, x);
            int n = ps.executeUpdate();
            if (n == 0) return ResponseEntity.notFound().build();
            return getById(x);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    // ANTI-PATTERN: delete also lives here.
    @DeleteMapping("/api/tasks/{x}")
    public Object delete(@PathVariable long x) {
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement("DELETE FROM tasks WHERE id=?")) {
            ps.setLong(1, x);
            int n = ps.executeUpdate();
            if (n == 0) return ResponseEntity.notFound().build();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    // Helper alias used above. ANTI-PATTERN: reusing routing method to return body.
    private Object getById(long id) {
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM tasks WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return ResponseEntity.notFound().build();
                return rowToMap(rs);
            }
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}

// ANTI-PATTERN: this class has no DTOs, no entities, no repository, no service.
// The whole project is a single file that does everything.
