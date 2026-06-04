package com.todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;

/**
 * ANTI-PATRON: god class.
 *
 * Esta unica clase se encarga de:
 *   - arrancar la aplicacion Spring
 *   - exponer todos los endpoints REST
 *   - abrir conexiones a SQLite en linea
 *   - ejecutar SQL DDL y DML
 *   - validar la entrada
 *   - convertir filas a Mapas
 *   - decidir los codigos HTTP
 *
 * No hay service, ni repository, ni DTO, ni entidad, ni interfaz.
 * Todo vive aqui. El mantenimiento es una pesadilla.
 */
@SpringBootApplication
@RestController
public class Main {

    // ANTI-PATRON: cadena de conexion hardcodeada, duplicada con application.properties.
    private static final String DB = "jdbc:sqlite:todo.db";

    public static void main(String[] args) throws Exception {
        // ANTI-PATRON: DDL embebido dentro del main.
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

    // ---- Helpers (tambien son anti-patrones) ----

    // ANTI-PATRON: logica de acceso a datos embebida en el controlador.
    private Connection getConn() throws SQLException {
        return DriverManager.getConnection(DB);
    }

    // ANTI-PATRON: SQL se filtra al controlador, no hay abstraccion de repository.
    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> m = new HashMap<>();
        m.put("id", rs.getLong("id"));
        m.put("title", rs.getString("title"));
        m.put("description", rs.getString("description"));
        m.put("completed", rs.getInt("completed") == 1);
        return m;
    }

    // ANTI-PATRON: validacion pegada al routing HTTP.
    private String validate(Map<String, Object> data) {
        Object t = data.get("title");
        if (t == null || t.toString().trim().isEmpty()) {
            return "title is required";
        }
        return null;
    }

    // ---- Endpoints (todos en una sola clase) ----

    // ANTI-PATRON: routing, validacion, persistencia y serializacion mezclados.
    @GetMapping("/api/tasks")
    public Object list() {
        try (Connection c = getConn();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM tasks")) {
            List<Map<String, Object>> out = new ArrayList<>();
            while (rs.next()) out.add(rowToMap(rs));
            return out;
        } catch (Exception e) {
            // ANTI-PATRON: catch generico que se traga la traza.
            return Map.of("error", e.getMessage());
        }
    }

    // ANTI-PATRON: parametro con nombre generico `data`, sin DTO.
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

    // ANTI-PATRON: parametro `x` y primitivo long expuesto tal cual.
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

    // ANTI-PATRON: logica de update mezclada con la capa HTTP.
    @PutMapping("/api/tasks/{x}")
    public Object update(@PathVariable long x, @RequestBody Map<String, Object> data) {
        try (Connection c = getConn()) {
            // comprobar que existe
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

    // ANTI-PATRON: PATCH (marcar como completada) implementado como otro god-method.
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

    // ANTI-PATRON: el delete tambien vive aqui.
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

    // ANTI-PATRON: se reutiliza un metodo de routing para devolver cuerpo.
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

// ANTI-PATRON: esta clase no tiene DTOs, ni entidades, ni repository, ni service.
// Todo el proyecto es un unico archivo que lo hace todo.
