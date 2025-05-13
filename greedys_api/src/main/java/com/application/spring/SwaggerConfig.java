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
                .addOpenApiCustomizer(groupCustomizer(adminTags(), "adminBearerAuth"))
                .build();
    }

    @Bean
    public GroupedOpenApi customerApi() {
        return GroupedOpenApi.builder()
                .group("customer-api")
                .packagesToScan("com.application.controller.customer")
                .pathsToMatch("/customer/**")
                .addOpenApiCustomizer(groupCustomizer(customerTags(), "customerBearerAuth"))
                .build();
    }

    @Bean
    public GroupedOpenApi restaurantApi() {
        return GroupedOpenApi.builder()
                .group("restaurant-api")
                .packagesToScan("com.application.controller.restaurant")
                .pathsToMatch("/restaurant/**")
                .addOpenApiCustomizer(groupCustomizer(restaurantTags(), "restaurantBearerAuth"))
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public-api")
                .packagesToScan("com.application.controller.pub", "com.application.web.dto")
                .pathsToMatch("/public/**", "/register/**", "/auth/**")
                .addOpenApiCustomizer(groupCustomizer(publicTags(), null))
                .build();
    }

    private OpenApiCustomizer groupCustomizer(List<Tag> tags, String securityName) {
        return openApi -> {
            tags.forEach(openApi::addTagsItem);
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

    private List<Tag> adminTags() {
        return List.of(
                new Tag().name("1. Admin Authentication").description("Admin Authentication Controller"),
                new Tag().name("2. Admin Registration").description("Admin Registration Management"),
                new Tag().name("3. Users").description("Admin Users Management"),
                new Tag().name("4. Customer").description("Admin Customer Management"),
                new Tag().name("5. Restaurant").description("Admin Restaurant Management"),
                new Tag().name("6. Services").description("Admin Services Management"),
                new Tag().name("7. Reservation").description("Admin Reservation Management")
        );
    }

    private List<Tag> customerTags() {
        return List.of(
                new Tag().name("1. Customer Authentication").description("Customer Authentication Controller"),
                new Tag().name("2. Customer Registration").description("Customer Registration Controller"),
                new Tag().name("3. Customer").description("Customer Data Controller"),
                new Tag().name("4. Reservation").description("Reservation Controller for Customers"),
                new Tag().name("5. Allergy").description("Customer Allergy Management"),
                new Tag().name("6. Notification").description("Customer Notification Controller")
        );
    }

    private List<Tag> restaurantTags() {
        return List.of(
                new Tag().name("1. Restaurant Authentication").description("Restaurant Auth Controller"),
                new Tag().name("2. Restaurant Registration").description("Restaurant Registration Controller"),
                new Tag().name("3. User Management").description("Restaurant User Management"),
                new Tag().name("4. Reservation Management").description("Restaurant Reservation Controller"),
                new Tag().name("5. Menu Management").description("Menu Management APIs"),
                new Tag().name("6. Notification Management").description("Notification APIs"),
                new Tag().name("7. Restaurant Management").description("Operations Management"),
                new Tag().name("8. Service Management").description("Service Management APIs"),
                new Tag().name("9. Slot Management").description("Slot Management APIs")
        );
    }

    private List<Tag> publicTags() {
        return List.of(
                new Tag().name("1. Restaurant").description("Public Restaurant APIs"),
                new Tag().name("2. Menu").description("Public Menu APIs")
        );
    }
}
