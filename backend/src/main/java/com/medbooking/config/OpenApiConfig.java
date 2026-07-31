package com.medbooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI medicalBookingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Medical Booking System API")
                        .version("1.0.0")
                        .description("RESTful API for Group 19's medical appointment booking system.")
                        .contact(new Contact().name("Group 19"))
                        .license(new License().name("Academic project")))
                .servers(List.of(new Server()
                        .url("http://localhost:8082")
                        .description("Local development server")));
    }
}
