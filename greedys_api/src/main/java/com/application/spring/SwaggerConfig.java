package com.application.spring;

import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI baseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Greedys API")
                        .version("1.0")
                        .description("API for managing restaurant reservations"))
                .components(baseComponents());
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin-api")
                .packagesToScan("com.application.controller.admin")
                .pathsToMatch("/admin/**")
                .addOpenApiCustomizer(groupCustomizer(null, "adminBearerAuth"))
                .build();
    }

    @Bean
    public GroupedOpenApi customerApi() {
        return GroupedOpenApi.builder()
                .group("customer-api")
                .packagesToScan("com.application.controller.customer")
                .pathsToMatch("/customer/**")
                .addOpenApiCustomizer(groupCustomizer(null, "customerBearerAuth"))
                .build();
    }

    @Bean
    public GroupedOpenApi restaurantApi() {
        return GroupedOpenApi.builder()
                .group("restaurant-api")
                .packagesToScan("com.application.controller.restaurant")
                .pathsToMatch("/restaurant/**")
                .addOpenApiCustomizer(groupCustomizer(null, "restaurantBearerAuth"))
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public-api")
                .packagesToScan("com.application.controller.pub", "com.application.web.dto")
                .pathsToMatch("/public/**", "/register/**", "/auth/**")
                .addOpenApiCustomizer(groupCustomizer(null, null))
                .build();
    }

    private OpenApiCustomizer groupCustomizer(List<Tag> tags, String securityName) {
        return openApi -> {
            if (tags != null) {
                tags.forEach(openApi::addTagsItem);
            }
            if (securityName != null) {
                openApi.addSecurityItem(new SecurityRequirement().addList(securityName));
                Components components = openApi.getComponents();
                if (components != null) {
                    if (components.getSecuritySchemes() == null || !components.getSecuritySchemes().containsKey(securityName)) {
                        components.addSecuritySchemes(securityName, bearerScheme());
                    }
                }
            }
        };
    }

    private Components baseComponents() {
        return new Components()
                .addSchemas("GenericResponse", new Schema<>()
                        .type("object")
                        .addProperty("message", new Schema<String>().type("string"))
                        .addProperty("error", new Schema<String>().type("string")))
                .addResponses("400", new ApiResponse().description("Bad Request"))
                .addResponses("401", new ApiResponse().description("Unauthorized"))
                .addResponses("403", new ApiResponse().description("Forbidden"))
                .addResponses("404", new ApiResponse().description("Not Found"))
                .addResponses("500", new ApiResponse().description("Internal Server Error"))
                .addResponses("405", new ApiResponse().description("Method Not Allowed"));
    }

    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }
}
