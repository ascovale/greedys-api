package com.application.common.spring;

import java.util.Map;
import java.util.TreeMap;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

    @Bean
    OpenAPI baseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Greedys API")
                        .version("1.0")
                        .description("API for managing restaurant reservations"))
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

    @Bean
    GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin-api")
                .packagesToScan("com.application.admin.controller")
                .pathsToMatch("/admin/**")
                .addOpenApiCustomizer(groupCustomizer())
                .build();
    }

    @Bean
    GroupedOpenApi customerApi() {
        return GroupedOpenApi.builder()
                .group("customer-api")
                .packagesToScan("com.application.customer.controller")
                .pathsToMatch("/customer/**")
                .addOpenApiCustomizer(groupCustomizer())
                .build();
    }

    @Bean
    GroupedOpenApi restaurantApi() {
        return GroupedOpenApi.builder()
                .group("restaurant-api")
                .packagesToScan("com.application.restaurant.controller", "com.application.restaurant.service")
                .pathsToMatch("/restaurant/**")
                .addOpenApiCustomizer(groupCustomizer())
                .build();
    }

    
    private OpenApiCustomizer groupCustomizer() {
        return openApi -> {
            // Configurazione security
            String securityName = "bearerAuth";
            openApi.addSecurityItem(new SecurityRequirement().addList(securityName));
            Components components = openApi.getComponents();
            if (components != null) {
                if (components.getSecuritySchemes() == null || !components.getSecuritySchemes().containsKey(securityName)) {
                    components.addSecuritySchemes(securityName, bearerScheme());
                }
            }
            
            // Fix per i generics - ServiceTypes endpoint
            fixServiceTypesEndpoint(openApi);
        };
    }
    
    private void fixServiceTypesEndpoint(OpenAPI openApi) {
        if (openApi.getPaths() != null && openApi.getPaths().get("/admin/service/types") != null) {
            var pathItem = openApi.getPaths().get("/admin/service/types");
            if (pathItem.getGet() != null && pathItem.getGet().getResponses() != null) {
                var response200 = pathItem.getGet().getResponses().get("200");
                if (response200 != null) {
                    // Crea schema specifico per ServiceTypeDto list
                    ObjectSchema responseSchema = new ObjectSchema();
                    responseSchema.addProperty("success", new Schema<>().type("boolean"));
                    responseSchema.addProperty("message", new Schema<>().type("string"));
                    responseSchema.addProperty("timestamp", new Schema<>().type("string").format("date-time"));
                    
                    // Array di ServiceTypeDto
                    ArraySchema dataArray = new ArraySchema();
                    dataArray.setItems(new Schema<>().$ref("#/components/schemas/ServiceTypeDto"));
                    responseSchema.addProperty("data", dataArray);
                    
                    // Metadata
                    responseSchema.addProperty("metadata", new Schema<>().$ref("#/components/schemas/ListMetadata"));
                    
                    // Applica lo schema
                    Content content = new Content();
                    MediaType mediaType = new MediaType();
                    mediaType.setSchema(responseSchema);
                    content.addMediaType("application/json", mediaType);
                    response200.setContent(content);
                }
            }
        }
    }

    private Components baseComponents() {
        return new Components()
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
