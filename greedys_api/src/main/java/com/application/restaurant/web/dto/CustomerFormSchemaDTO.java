package com.application.restaurant.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for customer form schema configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFormSchemaDTO {
    
    private JsonNode schema;

    /**
     * Check if schema is present and not null
     */
    public boolean hasSchema() {
        return schema != null && !schema.isNull();
    }

    /**
     * Get schema as string (for debugging/logging)
     */
    public String getSchemaAsString() {
        return schema != null ? schema.toString() : null;
    }

    /**
     * Create DTO with empty schema
     */
    public static CustomerFormSchemaDTO empty() {
        return CustomerFormSchemaDTO.builder()
            .schema(null)
            .build();
    }
}