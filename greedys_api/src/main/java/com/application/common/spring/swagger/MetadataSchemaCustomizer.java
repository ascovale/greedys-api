package com.application.common.spring.swagger;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;

@Component
public class MetadataSchemaCustomizer implements OpenApiCustomizer {

    @Override
    public void customise(io.swagger.v3.oas.models.OpenAPI openApi) {
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
    }
}
