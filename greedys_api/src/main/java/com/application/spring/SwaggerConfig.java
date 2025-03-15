package com.application.spring;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {
    @Bean
    OpenAPI customOpenAPI() {
        return new OpenAPI()
                .openapi("3.0.1") // Specify the OpenAPI version
                .info(new Info()
                .title("Greedys API")
                .version("1.0")
                .description("API for managing restaurant reservations"))c
                .components(new Components()
                        .addSecuritySchemes("adminBearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        .addSecuritySchemes("customerBearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        .addSecuritySchemes("restaurantBearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        .addResponses("400", new ApiResponse().description("Bad Request"))
                        .addResponses("401", new ApiResponse().description("Unauthorized"))
                        .addResponses("403", new ApiResponse().description("Forbidden"))
                        .addResponses("404", new ApiResponse().description("Not Found"))
                        .addResponses("500", new ApiResponse().description("Internal Server Error")));
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin-api")
                .pathsToMatch("/admin/**")
                .addOpenApiCustomizer(openApi -> openApi.addSecurityItem(new SecurityRequirement().addList("adminBearerAuth")))
                .build();
    }

    @Bean
    public GroupedOpenApi customerApi() {
        return GroupedOpenApi.builder()
                .group("customer-api")
                .pathsToMatch("/customer/**") 
                .addOpenApiCustomizer(openApi -> openApi.addSecurityItem(new SecurityRequirement().addList("customerBearerAuth")))
                .build();
    }

    @Bean
    public GroupedOpenApi restaurantApi() {
        return GroupedOpenApi.builder()
                .group("restaurant-api")
                .pathsToMatch("/restaurant_user/**")
                .addOpenApiCustomizer(openApi -> openApi.addSecurityItem(new SecurityRequirement().addList("restaurantBearerAuth")))
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public-api")
                .pathsToMatch("/public/**","/register/**","/auth/**")
                .build();
    }
}