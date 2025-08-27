package com.application.common.spring.swagger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig implements WebMvcConfigurer {

    // Package scanning configuration for DTOs - TEMPORARY: all DTOs until proper reorganization
    private static final List<String> ALL_DTO_PACKAGES = Arrays.asList(
        "com.application.admin.web.dto",
        "com.application.customer.web.dto", 
        "com.application.restaurant.web.dto",
        "com.application.common.web.dto"
    );

    /**
     * Configura il redirect per utilizzare la nostra interfaccia Swagger personalizzata
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect da /swagger-ui.html alla nostra interfaccia customizzata
        registry.addRedirectViewController("/swagger-ui.html", "/swagger-ui/index.html");
        // Non serve il redirect per /swagger-ui/ perché ora Spring serve direttamente i file statici
    }

    @Bean
    OpenAPI baseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Greedys API")
                        .version("1.0")
                        .description("API for managing restaurant reservations\n\n"))
                .components(baseComponents());
    }

    @Bean
    OpenApiCustomizer sortSchemasCustomizer() {
        return openApi -> {
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                @SuppressWarnings("rawtypes")
                Map<String, Schema> original = openApi.getComponents().getSchemas();
                @SuppressWarnings("rawtypes")
                Map<String, Schema> sorted = new TreeMap<>();
                for (@SuppressWarnings("rawtypes") Map.Entry<String, Schema> entry : original.entrySet()) {
                    sorted.put(entry.getKey(), entry.getValue());
                }
                openApi.getComponents().setSchemas(sorted);
            }
        };
    }

    // =========================================
    // ADMIN API BEANS
    // =========================================
    @Bean
    WrapperTypeRegistry adminWrapperTypeRegistry() {
        return new WrapperTypeRegistry("admin-api");
    }

    @Bean
    WrapperTypeOperationCustomizer adminWrapperTypeOperationCustomizer() {
        return new WrapperTypeOperationCustomizer(adminWrapperTypeRegistry());
    }

    @Bean
    WrapperTypeCustomizer adminWrapperTypeCustomizer() {
        return new WrapperTypeCustomizer("admin-api", adminWrapperTypeRegistry());
    }

    // =========================================
    // CUSTOMER API BEANS  
    // =========================================
    @Bean
    WrapperTypeRegistry customerWrapperTypeRegistry() {
        return new WrapperTypeRegistry("customer-api");
    }

    @Bean
    WrapperTypeOperationCustomizer customerWrapperTypeOperationCustomizer() {
        return new WrapperTypeOperationCustomizer(customerWrapperTypeRegistry());
    }

    @Bean
    WrapperTypeCustomizer customerWrapperTypeCustomizer() {
        return new WrapperTypeCustomizer("customer-api", customerWrapperTypeRegistry());
    }

    // =========================================
    // RESTAURANT API BEANS
    // =========================================
    @Bean
    WrapperTypeRegistry restaurantWrapperTypeRegistry() {
        return new WrapperTypeRegistry("restaurant-api");
    }

    @Bean
    WrapperTypeOperationCustomizer restaurantWrapperTypeOperationCustomizer() {
        return new WrapperTypeOperationCustomizer(restaurantWrapperTypeRegistry());
    }

    @Bean
    WrapperTypeCustomizer restaurantWrapperTypeCustomizer() {
        return new WrapperTypeCustomizer("restaurant-api", restaurantWrapperTypeRegistry());
    }

    @Bean
    GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin-api")
                .packagesToScan(
                    "com.application.admin.controller",
                    "com.application.common.controller",
                    ALL_DTO_PACKAGES.get(0), // admin.web.dto
                    ALL_DTO_PACKAGES.get(1), // customer.web.dto
                    ALL_DTO_PACKAGES.get(2), // restaurant.web.dto
                    ALL_DTO_PACKAGES.get(3)  // common.web.dto
                )
                .pathsToMatch("/admin/**")
                // OPERATION CUSTOMIZERS FIRST (populate extension data)
                .addOperationCustomizer(adminWrapperTypeOperationCustomizer())
                // OPENAPI CUSTOMIZERS SECOND (process extension data)
                .addOpenApiCustomizer(groupCustomizer())
                .addOpenApiCustomizer(sortSchemasCustomizer())
                .addOpenApiCustomizer(adminWrapperTypeCustomizer())
                .build();
    }

    @Bean
    GroupedOpenApi customerApi() {
        return GroupedOpenApi.builder()
                .group("customer-api")
                .packagesToScan(
                    "com.application.customer.controller",
                    "com.application.common.controller",
                    ALL_DTO_PACKAGES.get(1), // customer.web.dto
                    ALL_DTO_PACKAGES.get(3)  // common.web.dto
                )
                .pathsToMatch("/customer/**")
                // OPERATION CUSTOMIZERS FIRST (populate extension data)
                .addOperationCustomizer(customerWrapperTypeOperationCustomizer())
                // OPENAPI CUSTOMIZERS SECOND (process extension data)
                .addOpenApiCustomizer(groupCustomizer())
                .addOpenApiCustomizer(sortSchemasCustomizer())
                .addOpenApiCustomizer(customerWrapperTypeCustomizer())
                .build();
    }

    @Bean
    GroupedOpenApi restaurantApi() {
        return GroupedOpenApi.builder()
                .group("restaurant-api")
                .packagesToScan(
                    "com.application.restaurant.controller",
                    "com.application.common.controller",
                    ALL_DTO_PACKAGES.get(2), // restaurant.web.dto
                    ALL_DTO_PACKAGES.get(3)  // common.web.dto
                )
                .pathsToMatch("/restaurant/**")
                // OPERATION CUSTOMIZERS FIRST (populate extension data)
                .addOperationCustomizer(restaurantWrapperTypeOperationCustomizer())
                // OPENAPI CUSTOMIZERS SECOND (process extension data)
                .addOpenApiCustomizer(groupCustomizer())
                .addOpenApiCustomizer(sortSchemasCustomizer())
                .addOpenApiCustomizer(restaurantWrapperTypeCustomizer())
                .build();
    }

    
    private OpenApiCustomizer groupCustomizer() {
        return openApi -> {
            String securityName = "bearerAuth";
            openApi.addSecurityItem(new SecurityRequirement().addList(securityName));
            Components components = openApi.getComponents();
            if (components != null) {
                if (components.getSecuritySchemes() == null || !components.getSecuritySchemes().containsKey(securityName)) {
                    components.addSecuritySchemes(securityName, bearerScheme());
                }
            }
        };
    }

    private Components baseComponents() {
        Components components = new Components()
                .addResponses("400", new ApiResponse().description("Bad Request"))
                .addResponses("401", new ApiResponse().description("Unauthorized"))
                .addResponses("403", new ApiResponse().description("Forbidden"))
                .addResponses("404", new ApiResponse().description("Not Found"))
                .addResponses("500", new ApiResponse().description("Internal Server Error"))
                .addResponses("405", new ApiResponse().description("Method Not Allowed"));
                
        return components;
    }

    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }
}
