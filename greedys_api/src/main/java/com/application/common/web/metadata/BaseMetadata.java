package com.application.common.web.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Schema(
    name = "BaseMetadata", 
    description = "Metadata for API responses"
)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseMetadata {
    
    @Schema(description = "Additional metadata", type = "object")
    private Object additional;
    
    @Schema(description = "Whether the data is filtered", example = "false")
    private Boolean filtered;
    
    @Schema(description = "Applied filters description", example = "status=active")
    private String filterDescription;
    
    public static BaseMetadata create() {
        return BaseMetadata.builder()
                .filtered(false)
                .build();
    }
    
    public static BaseMetadata create(String filterDescription) {
        return BaseMetadata.builder()
                .filtered(filterDescription != null && !filterDescription.trim().isEmpty())
                .filterDescription(filterDescription)
                .build();
    }
    
    public static BaseMetadata create(boolean filtered, String filterDescription) {
        return BaseMetadata.builder()
                .filtered(filtered)
                .filterDescription(filterDescription)
                .build();
    }
    
    public static BaseMetadata create(Object additional) {
        return BaseMetadata.builder()
                .filtered(false)
                .additional(additional)
                .build();
    }
}
