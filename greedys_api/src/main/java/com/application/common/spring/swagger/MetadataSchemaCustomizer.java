package com.application.common.spring.swagger;

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
        
        // Ensure SingleMetadata schema exists
        if (!schemas.containsKey("SingleMetadata")) {
            Schema<?> singleMetadata = new Schema<>();
            singleMetadata.setType("object");
            singleMetadata.setDescription("Metadata for single object responses");
            
            // Add all properties directly (no allOf, no dataType)
            singleMetadata.addProperty("additional", new Schema<>().type("object").description("Additional metadata"));
            schemas.put("SingleMetadata", singleMetadata);
        }
        
        // Ensure ListMetadata schema exists  
        if (!schemas.containsKey("ListMetadata")) {
            Schema<?> listMetadata = new Schema<>();
            listMetadata.setType("object");
            listMetadata.setDescription("Metadata for list responses");
            
            // Add all properties directly (no allOf, no dataType)
            listMetadata.addProperty("additional", new Schema<>().type("object").description("Additional metadata"));
            listMetadata.addProperty("totalCount", new Schema<>().type("integer").format("int64").description("Total number of items in the list"));
            listMetadata.addProperty("count", new Schema<>().type("integer").format("int32").description("Number of items returned in this response"));
            listMetadata.addProperty("filtered", new Schema<>().type("boolean").description("Whether the list is filtered"));
            listMetadata.addProperty("filterDescription", new Schema<>().type("string").description("Applied filters description"));
            
            schemas.put("ListMetadata", listMetadata);
        }
        
        // Ensure PageMetadata schema exists
        if (!schemas.containsKey("PageMetadata")) {
            Schema<?> pageMetadata = new Schema<>();
            pageMetadata.setType("object");
            pageMetadata.setDescription("Metadata for paginated responses");
            
            // Add all properties directly (no allOf, no dataType)
            pageMetadata.addProperty("additional", new Schema<>().type("object").description("Additional metadata"));
            pageMetadata.addProperty("totalCount", new Schema<>().type("integer").format("int64").description("Total number of items across all pages"));
            pageMetadata.addProperty("count", new Schema<>().type("integer").format("int32").description("Number of items in current page"));
            pageMetadata.addProperty("page", new Schema<>().type("integer").format("int32").description("Current page number (0-based)"));
            pageMetadata.addProperty("size", new Schema<>().type("integer").format("int32").description("Number of items per page"));
            pageMetadata.addProperty("totalPages", new Schema<>().type("integer").format("int32").description("Total number of pages"));
            pageMetadata.addProperty("first", new Schema<>().type("boolean").description("Whether this is the first page"));
            pageMetadata.addProperty("last", new Schema<>().type("boolean").description("Whether this is the last page"));
            pageMetadata.addProperty("numberOfElements", new Schema<>().type("integer").format("int32").description("Number of elements in current page"));
            pageMetadata.addProperty("filtered", new Schema<>().type("boolean").description("Whether the list is filtered"));
            pageMetadata.addProperty("filterDescription", new Schema<>().type("string").description("Applied filters description"));
            
            schemas.put("PageMetadata", pageMetadata);
        }
    }
}
