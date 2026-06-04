package com.todo.infrastructure.config;

import com.todo.domain.port.in.TaskUseCase;
import com.todo.domain.service.TaskDomainService;
import com.todo.domain.port.out.TaskRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hexagonal composition root: wires the driving port to its
 * domain implementation, and binds the driven port to the
 * concrete adapter.
 *
 * <p>This is the <em>only</em> place where concrete classes from
 * domain and infrastructure are linked together. From the
 * outside, the application is just a set of ports.
 */
@Configuration
public class BeanConfig {

    @Bean
    public TaskUseCase taskUseCase(TaskRepository taskRepository) {
        return new TaskDomainService(taskRepository);
    }
}
