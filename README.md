# Branch `layered-monolith` — Arquitectura por capas

## Propósito
Esta rama refactoriza la aplicación de tareas aplicando una
**arquitectura en capas** estricta. Cada capa tiene una única
responsabilidad, y las dependencias entre ellas son **unidireccionales**
(de arriba hacia abajo):

```
Controller  →  Service  →  Repository  →  DB (SQLite)
```

> El objetivo es mostrar cómo separar concerns mejora la mantenibilidad,
> la testabilidad y la legibilidad, **sin** salir de un monolito.

## Diagrama de arquitectura

```
   HTTP request
        |
        v
+------------------------------------------------------------+
| controller/TaskController                                  |
|  - @RestController                                          |
|  - Solo concerns HTTP: status codes, DTOs, validacion       |
+----------------------------+-------------------------------+
                             |  usa DTOs (TaskRequest/Response)
                             v
+------------------------------------------------------------+
| service/TaskService                                        |
|  - @Service                                                 |
|  - Logica de negocio: findAllTasks, markAsCompleted, ...   |
|  - Transacciones (@Transactional)                           |
+----------------------------+-------------------------------+
                             |  usa entidades (Task)
                             v
+------------------------------------------------------------+
| repository/TaskRepository (Spring Data JPA)                |
|  - @Repository                                              |
|  - findAll, findById, save, deleteById                      |
+----------------------------+-------------------------------+
                             |
                             v
                       todo.db (SQLite)
```

## Responsabilidad de cada capa

| Capa           | Paquete          | Responsabilidad                                                       | No debe…                                                |
|----------------|------------------|------------------------------------------------------------------------|----------------------------------------------------------|
| **Controller** | `controller/`    | Deserializar HTTP, validar formato, elegir status code, serializar.   | Conocer JPA, JDBC, SQL, lógica de negocio.               |
| **Service**    | `service/`       | Reglas de negocio (`markAsCompleted`, `findTaskById`, transacciones).| Manejar `HttpServletRequest`, devolver DTOs al cliente.  |
| **Repository** | `repository/`    | Acceso a datos (CRUD JPA).                                             | Tener lógica de negocio, lanzar 404.                     |
| **Model**      | `model/`         | Entidad JPA `Task` (id, title, description, completed).                | Tener dependencias de Spring Web.                       |
| **DTO**        | `dto/`           | `TaskRequest` (entrada) y `TaskResponse` (salida).                     | Ser la entidad persistida.                              |
| **Exception**  | `exception/`     | `TaskNotFoundException`, `GlobalExceptionHandler` (mapea a HTTP).      | Acoplar lógica de negocio.                              |

## Regla de dependencia unidireccional
- `controller/` **solo importa** de `service/`, `dto/`, `exception/`.
- `service/` **solo importa** de `repository/`, `model/`, `exception/`, `dto/`.
- `repository/` **solo importa** de `model/`.
- `model/` no importa nada del proyecto.
- **Nunca** un paquete inferior importa de uno superior.

> En la rama `spaghetti` esta regla se rompía constantemente: el
> "controlador" conocía SQL, DDL, validación, Mapas y DTOs al mismo
> tiempo. Aquí, el controlador no sabe ni siquiera qué base de datos
> hay debajo.

## Lenguaje ubicuo (Ubiquitous Language)
Los nombres siguen el vocabulario del dominio:
- `findAllTasks`, `findTaskById`
- `createTask`, `updateTask`
- `markAsCompleted`
- `deleteTask`

Compara con la rama `spaghetti`, donde los métodos se llamaban
literalmente `get`, `update`, `complete`, `delete` mezclados en una
clase llamada `Main`.

## Cómo ejecutar
```bash
mvn clean
mvn -DskipTests package
mvn spring-boot:run
```
Levanta en `http://localhost:8080` y crea `todo.db` automáticamente.

## Endpoints

| Método | Ruta                       | Descripción                              | Códigos                |
|--------|----------------------------|------------------------------------------|------------------------|
| GET    | `/api/tasks`               | Listar todas las tareas.                 | 200                    |
| POST   | `/api/tasks`               | Crear tarea (`{title, description?}`).   | 201 / 400              |
| GET    | `/api/tasks/{id}`          | Obtener tarea por id.                    | 200 / 404              |
| PUT    | `/api/tasks/{id}`          | Reemplazar título/descripción/completed. | 200 / 404 / 400        |
| PATCH  | `/api/tasks/{id}/complete` | Marcar como completada.                  | 200 / 404              |
| DELETE | `/api/tasks/{id}`          | Eliminar tarea.                          | 200 / 404              |

### Ejemplos rápidos
```bash
curl -X POST http://localhost:8080/api/tasks \
     -H 'Content-Type: application/json' \
     -d '{"title":"Comprar pan","description":"en la tienda"}'

curl http://localhost:8080/api/tasks
curl -X PATCH http://localhost:8080/api/tasks/1/complete
curl -X DELETE http://localhost:8080/api/tasks/1
```

## Contraste con la rama `spaghetti`

| Aspecto                          | spaghetti                          | layered-monolith                          |
|----------------------------------|------------------------------------|-------------------------------------------|
| Número de clases                 | 1                                  | 8                                         |
| SQL en el controlador            | Sí                                 | No                                        |
| DTOs                             | No (usa `Map<String,Object>`)      | Sí (`TaskRequest`/`TaskResponse`)         |
| Validación                       | Manual, en línea                   | `@Valid` + `Bean Validation`              |
| Manejo de errores                | `catch (Exception)` que traga todo | `@RestControllerAdvice` mapea a 404/400   |
| Códigos HTTP                     | Mezclados con SQL                  | Decididos solo en el controlador          |
| Nombres de métodos               | `get`, `update`, `x`               | `markAsCompleted`, `findAllTasks`         |
| Probable de testear en aislamiento | No                                 | Sí (mockeando `TaskService`)              |

## Archivos de esta rama
```
pom.xml
src/main/resources/application.properties
src/main/java/com/todo/TodoApplication.java
src/main/java/com/todo/controller/TaskController.java
src/main/java/com/todo/service/TaskService.java
src/main/java/com/todo/repository/TaskRepository.java
src/main/java/com/todo/model/Task.java
src/main/java/com/todo/dto/TaskRequest.java
src/main/java/com/todo/dto/TaskResponse.java
src/main/java/com/todo/exception/TaskNotFoundException.java
src/main/java/com/todo/exception/GlobalExceptionHandler.java
README.md
```
