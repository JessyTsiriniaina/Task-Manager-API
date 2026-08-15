package io.jessytsiriniaina.taskmanagerapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(
        info = @Info(
                title = "Task Manager API",
                version = "1.0",
                description = "Documentation for Task API Manager"
        )
)

@SpringBootApplication
public class TaskManagerApiApplication {

    static void main(String[] args) {
        SpringApplication.run(TaskManagerApiApplication.class, args);
    }

}
