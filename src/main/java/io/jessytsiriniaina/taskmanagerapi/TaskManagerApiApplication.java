package io.jessytsiriniaina.taskmanagerapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(
        info = @Info(
                title = "Task Manager API",
                version = "1.0",
                description = "Task Manager API allows users to create, manage and organize their personal tasks. " +
                        "Each user only sees and operates on their own tasks. " +
                        "Most endpoints require a JWT access token obtained through the Authentication endpoints " +
                        "(register, login, refresh) and sent as 'Authorization: Bearer <token>'."
        )
)

@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT issued by the Authentication endpoints. " +
                "Send it as 'Authorization: Bearer <token>'."
)

@SpringBootApplication
public class TaskManagerApiApplication {

    static void main(String[] args) {
        SpringApplication.run(TaskManagerApiApplication.class, args);
    }

}
