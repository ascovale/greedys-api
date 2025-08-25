package com.application.common.web.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "SingleMetadata", description = "Metadata for single object responses")
public class SingleMetadata extends BaseMetadata {
    
    public static SingleMetadata create() {
        return SingleMetadata.builder()
                .build();
    }
    
    public static SingleMetadata create(Object additional) {
        return SingleMetadata.builder()
                .additional(additional)
                .build();
    }
}
