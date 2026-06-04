# Branch `ddd-hexagonal` — DDD + Arquitectura Hexagonal (Ports & Adapters)

## Propósito
Esta rama aplica **Domain-Driven Design (DDD)** junto con la
**Arquitectura Hexagonal** (también llamada "Ports & Adapters" o
"Clean Architecture") al mismo dominio de tareas.

La idea central es: **el dominio no sabe nada de Spring, ni de JPA,
ni de HTTP, ni de SQLite**. Todo eso vive en `infrastructure/`. El
núcleo (`domain/`) es Java puro, portable, y se testea sin
necesidad de levantar un contenedor.

## Diagrama de arquitectura

```
                       +--------------------------+
   HTTP / JSON         |   infrastructure/web/    |
   ------------------> |   TaskController         |  (driving adapter)
                       |   WebExceptionHandler    |
                       +-----------+--------------+
                                   |
                                   | depende del PUERTO (interfaz)
                                   v
                       +--------------------------+
                       |      domain/port/in/     |
                       |      TaskUseCase         |  <-- driving port
                       +-----------+--------------+
                                   | (implemented by)
                                   v
                       +--------------------------+
                       |   domain/service/        |
                       |   TaskDomainService      |  <-- logica de negocio
                       +-----------+--------------+
                                   |
                                   | depende del PUERTO (interfaz)
                                   v
                       +--------------------------+
                       |   domain/port/out/       |
                       |   TaskRepository         |  <-- driven port
                       +-----------+--------------+
                                   | (implemented by)
                                   v
                       +--------------------------+
                       | infrastructure/persist.  |
                       | SqliteTaskRepository     |  (driven adapter)
                       +-----------+--------------+
                                   |
                                   v
                              todo.db (SQLite)

   Composición:        infrastructure/config/BeanConfig.java
                       (el único lugar donde se enchufan los puertos
                        con sus adapters)
```

## ¿Qué es DDD?
Domain-Driven Design (Eric Evans, 2003) es una disciplina de diseño de
software que pone el **modelo del dominio** en el centro. En esta
rama se aplican los siguientes conceptos **tácticos**:

| Concepto DDD                     | Dónde se ve                                              |
|----------------------------------|----------------------------------------------------------|
| **Aggregate Root**               | `domain/model/Task` — entidad con identidad e invariantes. |
| **Entity**                       | `Task` (no es Value Object: tiene `id`).                 |
| **Ubiquitous Language**          | `markAsCompleted`, `updateDetails`, `renameTo`, `createNew`. |
| **Domain Service**               | `domain/service/TaskDomainService` — coordina casos de uso. |
| **Repository (port)**            | `domain/port/out/TaskRepository` — contrato de persistencia. |
| **Application Service / Use Case** | `domain/port/in/TaskUseCase` — el "qué" del sistema.    |
| **Domain Exception**             | `domain/model/TaskNotFoundException` — el dominio lanza, la infra traduce. |

> Esta rama no aplica explícitamente los conceptos **estratégicos** de
> DDD (Bounded Contexts, Context Map), porque hay un único contexto.
> Pero el código está organizado de forma que, si mañana se divide en
> "Tareas personales" y "Tareas de equipo", la estructura no obliga a
> reescribir nada.

## ¿Qué es la Arquitectura Hexagonal?
Propuesta por Alistair Cockburn, también llamada **Ports & Adapters**:

- **Puerto**: una **interfaz** Java que vive en `domain/`. Define un
  contrato del que la aplicación depende.
- **Adaptador**: una **implementación** de un puerto, que vive en
  `infrastructure/`. Conoce los detalles técnicos (HTTP, JDBC, etc.).
- **Driving port** (puerto de entrada): lo que el mundo exterior
  *pide* al dominio → `TaskUseCase`.
- **Driven port** (puerto de salida): lo que el dominio *necesita*
  del exterior → `TaskRepository`.

| Puerto                  | Tipo           | Adaptador                              |
|-------------------------|----------------|----------------------------------------|
| `TaskUseCase` (in)      | Driving port   | `infrastructure/web/TaskController`    |
| `TaskRepository` (out)  | Driven port    | `infrastructure/persistence/SqliteTaskRepository` |

## Explicación paquete por paquete

### `domain/`
- **`model/Task.java`** — entidad rica. Tiene comportamiento:
  `complete()`, `updateDetails(title, desc)`, `renameTo(title)`,
  `isCompleted()`. Las invariantes (título no vacío) se validan en
  el constructor. **No es un simple DTO con getters.**
- **`model/TaskNotFoundException.java`** — excepción del dominio, sin
  dependencias de Spring.
- **`port/in/TaskUseCase.java`** — interfaz de los casos de uso.
  Cualquier adaptador (REST, CLI, gRPC, batch…) puede implementarla.
- **`port/out/TaskRepository.java`** — interfaz de persistencia.
- **`service/TaskDomainService.java`** — implementa `TaskUseCase`.
  Orquesta el modelo y el repositorio.

### `infrastructure/`
- **`persistence/SqliteTaskRepository.java`** — adaptador JDBC que
  implementa `TaskRepository`. Aquí vive el SQL, el `JdbcTemplate`,
  el `RowMapper`. La creación del esquema (`CREATE TABLE IF NOT
  EXISTS`) también va aquí, no en el dominio.
- **`web/TaskController.java`** — adaptador REST. Depende de
  `TaskUseCase` (la interfaz), **nunca** de `SqliteTaskRepository`
  ni de la entidad JPA.
- **`web/TaskDto.java`**, **`CreateTaskRequest.java`**,
  **`UpdateTaskRequest.java`** — DTOs de transporte.
- **`web/WebExceptionHandler.java`** — traduce excepciones del
  dominio a códigos HTTP (404, 400). Es código de adaptador, no
  de dominio.
- **`config/BeanConfig.java`** — el "composition root". Aquí se
  enchufa `TaskRepository` (puerto) con `SqliteTaskRepository`
  (adaptador) y se expone `TaskUseCase` como bean de Spring.

## ¿Por qué el dominio no tiene dependencias de framework?
- **Testabilidad**: `Task` y `TaskDomainService` se prueban con JUnit
  puro, sin `@SpringBootTest`, sin levantar Tomcat, sin base de datos.
- **Portabilidad**: si mañana decides usar Postgres, MongoDB, Kafka
  o gRPC, **solo cambian los adaptadores**. El dominio ni se entera.
- **Estabilidad**: el dominio cambia cuando cambia el negocio, no
  cuando cambia Spring Boot de 3.2 a 3.5.
- **Claridad**: leer `Task.java` debe bastar para entender qué es
  una tarea en este negocio.

Para verificarlo, este es el conjunto de imports en `domain/`:
```
domain/model/Task.java              -> java.util.Objects
domain/port/in/TaskUseCase.java     -> domain.model.Task, java.util.List
domain/port/out/TaskRepository.java -> domain.model.Task, java.util.{List, Optional}
domain/service/TaskDomainService.java -> domain.model.*, domain.port.*
```
**Cero** `import org.springframework.*` o `import jakarta.persistence.*`.

## Cómo ejecutar
```bash
mvn spring-boot:run
```
Levanta en `http://localhost:8080` y crea `todo.db` en la raíz
del proyecto (lo inicializa el adaptador SQLite, no el dominio).

## Endpoints

| Método | Ruta                       | Descripción                              | Códigos                |
|--------|----------------------------|------------------------------------------|------------------------|
| GET    | `/api/tasks`               | Listar todas las tareas.                 | 200                    |
| POST   | `/api/tasks`               | Crear tarea (`{title, description?}`).   | 201 / 400              |
| GET    | `/api/tasks/{id}`          | Obtener tarea por id.                    | 200 / 404              |
| PUT    | `/api/tasks/{id}`          | Actualizar título/descripción.           | 200 / 404 / 400        |
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

## Tabla comparativa de las tres ramas

| Aspecto                              | `spaghetti`              | `layered-monolith`               | `ddd-hexagonal`                                       |
|--------------------------------------|--------------------------|----------------------------------|-------------------------------------------------------|
| **Archivos Java**                    | 1                        | 8                                | 11                                                    |
| **Capas / módulos**                  | 0 (todo mezclado)        | 4 (controller/service/repo/model) | 2 (domain + infrastructure)                          |
| **Framework en el dominio**          | Sí (Spring, JDBC)        | Sí (JPA)                         | **No** (Java puro)                                    |
| **Entidad de dominio con métodos**   | No (es un `Map`)         | No (POJO JPA)                    | **Sí** (`complete()`, `updateDetails()`, …)            |
| **Persistencia intercambiable**      | No                       | No (acoplado a JPA)              | **Sí** (cambiar el adaptador basta)                   |
| **Interfaz que el controlador usa**   | N/A                      | `TaskService` (clase)            | `TaskUseCase` (interfaz)                              |
| **Lenguaje ubicuo**                  | Débil (`get`, `update`)  | Bueno (`markAsCompleted`)        | **Excelente** (`complete`, `updateDetails`)           |
| **Validación de invariantes**        | En el controller         | En el service                    | **En la entidad** (constructor)                       |
| **Excepciones**                      | `catch (Exception)`      | `TaskNotFoundException` + handler | `TaskNotFoundException` (dominio) + handler (infra)  |
| **Costo de añadir un campo a Task**  | Tocar todos los métodos  | Tocar entity + DTOs              | Tocar `Task` + DTOs + row mapper                      |
| **Testabilidad del núcleo**          | Nula                     | Media (con @MockBean)            | **Alta** (sin Spring)                                 |
| **Acoplamiento a SQLite**            | Alto (URL en el código)  | Medio (driver en properties)     | Bajo (cambiar `SqliteTaskRepository` por otro)        |
| **Apto para crecer a múltiples BCs**  | No                       | Poco                             | **Sí** (los puertos son la frontera natural)          |

## Archivos de esta rama
```
pom.xml
src/main/resources/application.properties
src/main/java/com/todo/TodoApplication.java

# Domain (Java puro)
src/main/java/com/todo/domain/model/Task.java
src/main/java/com/todo/domain/model/TaskNotFoundException.java
src/main/java/com/todo/domain/port/in/TaskUseCase.java
src/main/java/com/todo/domain/port/out/TaskRepository.java
src/main/java/com/todo/domain/service/TaskDomainService.java

# Infrastructure (adaptadores + wiring)
src/main/java/com/todo/infrastructure/persistence/SqliteTaskRepository.java
src/main/java/com/todo/infrastructure/web/TaskController.java
src/main/java/com/todo/infrastructure/web/TaskDto.java
src/main/java/com/todo/infrastructure/web/CreateTaskRequest.java
src/main/java/com/todo/infrastructure/web/UpdateTaskRequest.java
src/main/java/com/todo/infrastructure/web/WebExceptionHandler.java
src/main/java/com/todo/infrastructure/config/BeanConfig.java

README.md
```
