package com.todo.service;

import com.todo.dto.TaskRequest;
import com.todo.model.Task;
import com.todo.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<Task> findAllTasks() {
        return taskRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Task findTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new com.todo.exception.TaskNotFoundException(id));
    }

    @Transactional
    public Task createTask(TaskRequest request) {
        Task task = new Task(request.getTitle(), request.getDescription());
        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTask(Long id, TaskRequest request) {
        Task task = findTaskById(id);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getCompleted() != null) {
            task.setCompleted(request.getCompleted());
        }
        return task;
    }

    @Transactional
    public Task markAsCompleted(Long id) {
        Task task = findTaskById(id);
        task.setCompleted(true);
        return task;
    }

    @Transactional
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new com.todo.exception.TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
    }
}
