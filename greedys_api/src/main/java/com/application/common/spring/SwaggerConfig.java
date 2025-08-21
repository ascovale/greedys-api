package com.application.common.spring;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springdoc.core.customizers.OpenApiCustomizer;
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

@Configuration
public class SwaggerConfig {

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

    @Bean
    OpenApiCustomizer metadataSchemaCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            
            @SuppressWarnings("rawtypes")
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            if (schemas == null) {
                schemas = new TreeMap<>();
                openApi.getComponents().setSchemas(schemas);
            }
            
            // Ensure BaseMetadata discriminator schema exists
            if (!schemas.containsKey("BaseMetadata")) {
                Schema<?> baseMetadata = new Schema<>();
                baseMetadata.setType("object");
                baseMetadata.setDescription("Base metadata for API responses");
                
                // Add discriminator
                io.swagger.v3.oas.models.media.Discriminator discriminator = new io.swagger.v3.oas.models.media.Discriminator();
                discriminator.setPropertyName("dataType");
                discriminator.setMapping(Map.of(
                    "single", "#/components/schemas/SingleMetadata",
                    "list", "#/components/schemas/ListMetadata", 
                    "page", "#/components/schemas/PageMetadata"
                ));
                baseMetadata.setDiscriminator(discriminator);
                
                // Add properties
                baseMetadata.addProperty("dataType", new Schema<>().type("string").description("Type of data returned"));
                baseMetadata.addProperty("additional", new Schema<>().type("object").description("Additional metadata"));
                
                schemas.put("BaseMetadata", baseMetadata);
            }
            
            // Ensure SingleMetadata schema exists
            if (!schemas.containsKey("SingleMetadata")) {
                Schema<?> singleMetadata = new Schema<>();
                singleMetadata.setType("object");
                singleMetadata.setDescription("Metadata for single object responses");
                
                // Add allOf reference to BaseMetadata
                Schema<?> baseRef = new Schema<>();
                baseRef.set$ref("#/components/schemas/BaseMetadata");
                singleMetadata.setAllOf(List.of(baseRef));
                
                schemas.put("SingleMetadata", singleMetadata);
            }
            
            // Ensure ListMetadata schema exists  
            if (!schemas.containsKey("ListMetadata")) {
                Schema<?> listMetadata = new Schema<>();
                listMetadata.setType("object");
                listMetadata.setDescription("Metadata for list responses");
                
                // Add allOf reference to BaseMetadata
                Schema<?> baseRef = new Schema<>();
                baseRef.set$ref("#/components/schemas/BaseMetadata");
                
                Schema<?> listProperties = new Schema<>();
                listProperties.setType("object");
                listProperties.addProperty("totalCount", new Schema<>().type("integer").format("int64").description("Total number of items"));
                listProperties.addProperty("count", new Schema<>().type("integer").description("Number of items in response"));
                listProperties.addProperty("filtered", new Schema<>().type("boolean").description("Whether the list is filtered"));
                listProperties.addProperty("filterDescription", new Schema<>().type("string").description("Applied filters description"));
                
                listMetadata.setAllOf(List.of(baseRef, listProperties));
                schemas.put("ListMetadata", listMetadata);
            }
            
            // Ensure PageMetadata schema exists
            if (!schemas.containsKey("PageMetadata")) {
                Schema<?> pageMetadata = new Schema<>();
                pageMetadata.setType("object");
                pageMetadata.setDescription("Metadata for paginated responses");
                
                // Add allOf reference to BaseMetadata
                Schema<?> baseRef = new Schema<>();
                baseRef.set$ref("#/components/schemas/BaseMetadata");
                
                Schema<?> pageProperties = new Schema<>();
                pageProperties.setType("object");
                pageProperties.addProperty("totalCount", new Schema<>().type("integer").format("int64").description("Total number of items"));
                pageProperties.addProperty("count", new Schema<>().type("integer").description("Number of items in current page"));
                pageProperties.addProperty("pageNumber", new Schema<>().type("integer").description("Current page number"));
                pageProperties.addProperty("pageSize", new Schema<>().type("integer").description("Items per page"));
                pageProperties.addProperty("totalPages", new Schema<>().type("integer").description("Total number of pages"));
                pageProperties.addProperty("isFirst", new Schema<>().type("boolean").description("Whether this is the first page"));
                pageProperties.addProperty("isLast", new Schema<>().type("boolean").description("Whether this is the last page"));
                pageProperties.addProperty("hasNext", new Schema<>().type("boolean").description("Whether there is a next page"));
                pageProperties.addProperty("hasPrevious", new Schema<>().type("boolean").description("Whether there is a previous page"));
                
                pageMetadata.setAllOf(List.of(baseRef, pageProperties));
                schemas.put("PageMetadata", pageMetadata);
            }
        };
    }

    @Bean
    WrapperTypeOperationCustomizer wrapperTypeOperationCustomizer() {
        return new WrapperTypeOperationCustomizer();
    }

    @Bean
    WrapperTypeCustomizer wrapperTypeCustomizer() {
        return new WrapperTypeCustomizer();
    }

    @Bean
    GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin-api")
                .packagesToScan("com.application.admin","com.application.common", "com.application.restaurant", "com.application.customer")
                .pathsToMatch("/admin/**")
                .addOpenApiCustomizer(groupCustomizer())
                .addOpenApiCustomizer(sortSchemasCustomizer())
                .addOpenApiCustomizer(metadataSchemaCustomizer())
                .addOpenApiCustomizer(wrapperTypeCustomizer())
                .addOperationCustomizer(wrapperTypeOperationCustomizer())
                .build();
    }

    @Bean
    GroupedOpenApi customerApi() {
        return GroupedOpenApi.builder()
                .group("customer-api")
                .packagesToScan("com.application.admin","com.application.common", "com.application.restaurant", "com.application.customer")
                .pathsToMatch("/customer/**")
                .addOpenApiCustomizer(groupCustomizer())
                .addOpenApiCustomizer(sortSchemasCustomizer())
                .addOpenApiCustomizer(metadataSchemaCustomizer())
                .addOpenApiCustomizer(wrapperTypeCustomizer())
                .addOperationCustomizer(wrapperTypeOperationCustomizer())
                .build();
    }

    @Bean
    GroupedOpenApi restaurantApi() {
        return GroupedOpenApi.builder()
                .group("restaurant-api")
                .packagesToScan("com.application.admin","com.application.common", "com.application.restaurant", "com.application.customer")
                .pathsToMatch("/restaurant/**")
                .addOpenApiCustomizer(groupCustomizer())
                .addOpenApiCustomizer(sortSchemasCustomizer())
                .addOpenApiCustomizer(metadataSchemaCustomizer())
                .addOpenApiCustomizer(wrapperTypeCustomizer())
                .addOperationCustomizer(wrapperTypeOperationCustomizer())
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
