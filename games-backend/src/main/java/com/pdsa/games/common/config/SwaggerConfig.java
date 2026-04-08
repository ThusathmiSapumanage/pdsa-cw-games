package com.pdsa.games.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PDSA Games API")
                        .description("REST API documentation for the PDSA games backend. "
                                + "Error responses follow RFC 7807 and are returned as application/problem+json.")
                        .version("v1")
                        .contact(new Contact().name("PDSA Team"))
                        .license(new License().name("Apache 2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project README")
                        .url("/README.md"));
    }
}