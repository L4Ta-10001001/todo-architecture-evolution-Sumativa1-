# Branch `spaghetti` — Anti-patrones a propósito

## Propósito
Esta rama contiene la **peor versión posible** de la aplicación de tareas
(intencionalmente). Sirve como ejemplo pedagógico de lo que **NO** se debe
hacer al estructurar un proyecto Java/Spring. Toda la lógica vive en una
única clase `Main.java` que mezcla bootstrap, routing, validación, SQL y
negocio.

> "Spaghetti code" = código enredado donde el flujo de control, los datos y
> las responsabilidades están tan entrelazados que seguir la pista de una
> sola operación requiere saltar entre métodos, sin ningún tipo de
> separación de capas.

## Diagrama de "arquitectura"
```
+-----------------------------------------------------------+
|                      Main.java (God class)                |
|                                                           |
|  +-----------+   +-------------+   +-------------------+  |
|  | @RestCtrl |   |  validate() |   |  DriverManager    |  |
|  |  routes   |   |  rowToMap() |   |  + raw SQL DDL    |  |
|  |           |   |  getConn()  |   |  + raw SQL DML    |  |
|  +-----------+   +-------------+   +-------------------+  |
|         \             |              /                    |
|          \            |             /                     |
|           +-----------v------------+                      |
|           |    HTTP + SQL + Lógica  |                     |
|           |    todo mezclado        |                     |
|           +-----------+------------+                      |
|                       |                                   |
|                       v                                   |
|                 todo.db (SQLite)                          |
+-----------------------------------------------------------+
```

## Anti-patrones presentes (y dónde)

| # | Anti-patrón                                      | Dónde se ve                                                    |
|---|--------------------------------------------------|----------------------------------------------------------------|
| 1 | **God class**                                    | Toda la app es `Main.java` (Spring + REST + SQL + validación). |
| 2 | **Lógica de negocio en el controlador**          | `validate()`, `rowToMap()` están dentro de `@RestController`.  |
| 3 | **SQL hardcodeado en el controlador**            | `INSERT`, `UPDATE`, `DELETE`, `SELECT` dentro de `@PostMapping` etc. |
| 4 | **Cadena de conexión hardcodeada y duplicada**   | `private static final String DB = "jdbc:sqlite:todo.db";` y además en `application.properties`. |
| 5 | **Sin DTOs**                                     | Los endpoints reciben y devuelven `Map<String,Object>`.        |
| 6 | **Sin entidad de dominio**                       | No existe `Task`; las filas se convierten ad-hoc en cada método. |
| 7 | **Nombres genéricos**                            | `data`, `obj`, `temp`, `x`, `m`, `n`.                          |
| 8 | **Sin abstracciones / interfaces**               | Imposible mockear, imposible cambiar la persistencia.         |
| 9 | **Catch genérico que se traga la traza**         | `catch (Exception e) { return Map.of("error", e.getMessage()); }` |
| 10| **DDL embebido en `main`**                        | `CREATE TABLE IF NOT EXISTS tasks (...)` dentro de `public static void main`. |
| 11| **Mezcla verbos HTTP conmutados**                 | `PUT` reescribe, `PATCH /complete` reescribe, `DELETE` borra — todo en el mismo archivo. |

Cada uno de estos puntos está **etiquetado en el código** con un
comentario `// ANTI-PATTERN: ...` para que el lector los pueda localizar
al instante.

## Por qué es problemático para el mantenimiento
- **Testabilidad nula**: no se puede aislar la lógica de negocio
  porque está cosida al transporte HTTP y a JDBC.
- **Inversión de dependencias rota**: el controlador "conoce" la base
  de datos, el SQL, los nombres de columnas, y la forma de los Mapas.
  Cambiar SQLite por Postgres, o REST por gRPC, implica reescribir
  todo.
- **Alto acoplamiento, baja cohesión**: cada `@Mapping` hace de todo,
  por lo que añadir una columna nueva obliga a tocar todos los métodos.
- **Lectura hostil**: los nombres `x`, `data`, `obj` obligan al lector
  a reconstruir mentalmente el dominio en cada línea.
- **Cero Ubiquitous Language**: no hay verbos de dominio
  (`markAsCompleted`, `renameTask`); solo operaciones CRUD genéricas.
- **Refactorización arriesgada**: al estar todo en un archivo, el
  riesgo de regresiones al mover código es altísimo.

## Cómo ejecutar
```bash
mvn spring-boot:run
```
La aplicación levanta en `http://localhost:8080` y crea `todo.db` en la
raíz del proyecto.

## Endpoints

| Método | Ruta                       | Descripción                              | Códigos |
|--------|----------------------------|------------------------------------------|---------|
| GET    | `/api/tasks`               | Listar todas las tareas.                 | 200     |
| POST   | `/api/tasks`               | Crear tarea (`{title, description?}`).   | 200/400 |
| GET    | `/api/tasks/{id}`          | Obtener tarea por id.                    | 200/404 |
| PUT    | `/api/tasks/{id}`          | Reemplazar título/descripción/completed. | 200/404 |
| PATCH  | `/api/tasks/{id}/complete` | Marcar como completada.                  | 200/404 |
| DELETE | `/api/tasks/{id}`          | Eliminar tarea.                          | 200/404 |

### Ejemplos rápidos
```bash
curl -X POST http://localhost:8080/api/tasks \
     -H 'Content-Type: application/json' \
     -d '{"title":"Comprar pan","description":"En la tienda de la esquina"}'

curl http://localhost:8080/api/tasks
curl http://localhost:8080/api/tasks/1
curl -X PATCH http://localhost:8080/api/tasks/1/complete
curl -X DELETE http://localhost:8080/api/tasks/1
```

## Archivos de esta rama
- `pom.xml` — dependencias (web + JDBC + sqlite-jdbc).
- `src/main/resources/application.properties` — puerto y datasource.
- `src/main/java/com/todo/Main.java` — **toda** la aplicación.
- `README.md` — este documento.
