package com.application.common.web.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "dataType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SingleMetadata.class, name = "single"),
    @JsonSubTypes.Type(value = ListMetadata.class, name = "list"),
    @JsonSubTypes.Type(value = PageMetadata.class, name = "page")
})
@Schema(
    name = "BaseMetadata", 
    description = "Base metadata for API responses",
    discriminatorProperty = "dataType"
)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseMetadata {
    
    @Schema(description = "Type of data returned", example = "single")
    private String dataType;
    
    @Schema(description = "Additional metadata")
    private Object additional;
}
