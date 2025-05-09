package com.application.spring;

import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
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
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Greedys API")
                        .version("1.0")
                        .description("API for managing restaurant reservations"))
                .components(sharedComponents());
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return createGroupedOpenApi("admin-api", "/admin/**", adminTags(), "adminBearerAuth");
    }

    @Bean
    public GroupedOpenApi customerApi() {
        return createGroupedOpenApi("customer-api", "/customer/**", customerTags(), "customerBearerAuth");
    }

    @Bean
    public GroupedOpenApi restaurantApi() {
        return createGroupedOpenApi("restaurant-api", "/restaurant/**", restaurantTags(), "restaurantBearerAuth");
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public-api")
                .packagesToScan("com.application.controller.pub")
                .pathsToMatch("/public/**", "/register/**", "/auth/**")
                .pathsToExclude("/restaurant/**", "/customer/**", "/admin/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .components(sharedComponents())
                        .tags(publicTags()))
                .build();
    }

    private GroupedOpenApi createGroupedOpenApi(String group, String path, List<Tag> tags, String securityScheme) {
        return GroupedOpenApi.builder()
                .group(group)
                .pathsToMatch(path)
                .addOpenApiCustomizer(openApi -> openApi
                        .components(sharedComponents().addSecuritySchemes(securityScheme, securitySchemeConfig()))
                        .addSecurityItem(new SecurityRequirement().addList(securityScheme))
                        .tags(tags))
                .build();
    }

    private Components sharedComponents() {
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

    private SecurityScheme securitySchemeConfig() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }

    private List<Tag> adminTags() {
        return List.of(
                new Tag().name("1. Authentication").description("Admin Authentication Controller"),
                new Tag().name("2. Registration").description("Admin Registration Management"),
                new Tag().name("3. Users").description("Admin Users Management"),
                new Tag().name("4. Customer").description("Admin Customer Management"),
                new Tag().name("5. Restaurant").description("Admin Restaurant Management"),
                new Tag().name("6. Services").description("Admin Services Management"),
                new Tag().name("7. Reservation").description("Admin Reservation Management")
        );
    }

    private List<Tag> customerTags() {
        return List.of(
                new Tag().name("1. Authentication").description("Controller for managing customer authentication"),
                new Tag().name("2. Registration").description("Controller for managing customer registration"),
                new Tag().name("3. Customer").description("Controller for managing customers"),
                new Tag().name("4. Reservation").description("APIs for managing reservations of the customer"),
                new Tag().name("5. Allergy").description("Controller for managing customer allergies"),
                new Tag().name("6. Notification").description("Notification management APIs for customers")
        );
    }

    private List<Tag> restaurantTags() {
        return List.of(
                new Tag().name("1. Authentication").description("Controller for restaurant creation and user authentication"),
                new Tag().name("2. Registration").description("Controller for restaurant registration"),
                new Tag().name("3. User Management").description("Controller for managing restaurant users"),
                new Tag().name("4. Reservation Management").description("APIs for managing reservations from the restaurant"),
                new Tag().name("5. Menu Management").description("Restaurant Menu Controller APIs"),
                new Tag().name("6. Notification Management").description("Restaurant Notification management APIs"),
                new Tag().name("7. Restaurant Management").description("Controller for managing restaurant operations"),
                new Tag().name("8. Service Management").description("Controller for managing services offered by restaurants"),
                new Tag().name("9. Slot Management").description("Controller for managing slots")
        );
    }

    private List<Tag> publicTags() {
        return List.of(
                new Tag().name("1. Restaurant").description("Controller for managing restaurants"),
                new Tag().name("2. Menu").description("Restaurant Menu Controller APIs")
        );
    }
}
