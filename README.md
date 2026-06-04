# Todo App — Evolución Arquitectónica

API REST de tareas (Task Manager) implementada **3 veces** en 3 ramas
de Git, cada una con un estilo arquitectónico distinto sobre el mismo
dominio. Sirve como material comparativo de cómo las decisiones de
arquitectura impactan la mantenibilidad del código.

> El README detallado de cada rama vive dentro de la propia rama
> (no en `main`). Aquí está el resumen rápido para ubicarse en 30s.

## Ramas

| Rama | Estilo | Archivos Java | Resumen |
|---|---|---:|---|
| [`spaghetti`](https://github.com/L4Ta-10001001/todo-architecture-evolution-Sumativa1-/tree/spaghetti) | God class | 1 | Una sola clase hace todo (routing, SQL, validación). A propósito anti-patrones. |
| [`layered-monolith`](https://github.com/L4Ta-10001001/todo-architecture-evolution-Sumativa1-/tree/layered-monolith) | Capas | 8 | Controller → Service → Repository → DB. Spring Data JPA. |
| [`ddd-hexagonal`](https://github.com/L4Ta-10001001/todo-architecture-evolution-Sumativa1-/tree/ddd-hexagonal) | DDD + Hexagonal | 11 | Dominio puro (cero Spring/JPA), puertos in/out, adaptadores de infra. |

## Cómo correr cualquier rama

```bash
git checkout <nombre-rama>     # spaghetti | layered-monolith | ddd-hexagonal
rm -f todo.db                  # opcional: empieza con DB limpia
mvn spring-boot:run            # arranca en http://localhost:8080
```

Las 3 exponen la misma API en `/api/tasks`:

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/tasks` | Listar todas |
| POST | `/api/tasks` | Crear (body: `{title, description?}`) |
| GET | `/api/tasks/{id}` | Obtener por id |
| PUT | `/api/tasks/{id}` | Actualizar |
| PATCH | `/api/tasks/{id}/complete` | Marcar completada |
| DELETE | `/api/tasks/{id}` | Eliminar |

Para una explicación a fondo de cada estilo, abre el `README.md`
de la rama correspondiente.

## Comparación rápida

| Aspecto | `spaghetti` | `layered-monolith` | `ddd-hexagonal` |
|---|---|---|---|
| Capas / módulos | 0 | 4 | 2 (domain + infra) |
| Framework en el dominio | sí (JDBC) | sí (JPA) | **no** (Java puro) |
| Entidad con comportamiento | no | no | **sí** (`complete()`, `updateDetails()`) |
| Persistencia intercambiable | no | no (acoplado a JPA) | **sí** (cambiar el adaptador) |
| Testeable el núcleo | no | media | **alta** (sin Spring) |
| Lenguaje ubicuo | débil | bueno | **excelente** |

## Stack

- Java 17 (probado también con JDK 21)
- Spring Boot 3.2.5
- SQLite (`sqlite-jdbc` 3.45.3.0) → `todo.db` en la raíz
- Maven
