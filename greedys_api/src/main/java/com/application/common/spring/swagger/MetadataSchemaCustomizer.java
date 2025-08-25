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
            
            // Add properties with dataType as required
            baseMetadata.addProperty("dataType", new Schema<>().type("string").description("Type of data returned").example("single"));
            baseMetadata.addProperty("additional", new Schema<>().type("object").description("Additional metadata"));
            baseMetadata.addRequiredItem("dataType");
            
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
            
            // Add the dataType property as required
            Schema<?> singleProperties = new Schema<>();
            singleProperties.setType("object");
            singleProperties.addProperty("dataType", new Schema<>().type("string").example("single"));
            singleProperties.addRequiredItem("dataType");
            
            singleMetadata.setAllOf(List.of(baseRef, singleProperties));
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
            listProperties.addProperty("dataType", new Schema<>().type("string").example("list"));
            listProperties.addProperty("totalCount", new Schema<>().type("integer").format("int64").description("Total number of items in the list"));
            listProperties.addProperty("count", new Schema<>().type("integer").format("int32").description("Number of items returned in this response"));
            listProperties.addProperty("filtered", new Schema<>().type("boolean").description("Whether the list is filtered"));
            listProperties.addProperty("filterDescription", new Schema<>().type("string").description("Applied filters description"));
            listProperties.addRequiredItem("dataType");
            
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
            pageProperties.addProperty("dataType", new Schema<>().type("string").example("page"));
            pageProperties.addProperty("totalCount", new Schema<>().type("integer").format("int64").description("Total number of items across all pages"));
            pageProperties.addProperty("count", new Schema<>().type("integer").format("int32").description("Number of items in current page"));
            pageProperties.addProperty("page", new Schema<>().type("integer").format("int32").description("Current page number (0-based)"));
            pageProperties.addProperty("size", new Schema<>().type("integer").format("int32").description("Number of items per page"));
            pageProperties.addProperty("totalPages", new Schema<>().type("integer").format("int32").description("Total number of pages"));
            pageProperties.addProperty("first", new Schema<>().type("boolean").description("Whether this is the first page"));
            pageProperties.addProperty("last", new Schema<>().type("boolean").description("Whether this is the last page"));
            pageProperties.addProperty("numberOfElements", new Schema<>().type("integer").format("int32").description("Number of elements in current page"));
            pageProperties.addProperty("filtered", new Schema<>().type("boolean").description("Whether the list is filtered"));
            pageProperties.addProperty("filterDescription", new Schema<>().type("string").description("Applied filters description"));
            pageProperties.addRequiredItem("dataType");
            
            pageMetadata.setAllOf(List.of(baseRef, pageProperties));
            schemas.put("PageMetadata", pageMetadata);
        }
    }
}
