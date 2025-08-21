package com.application.common.web.metadata;

import java.util.List;

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
@Schema(name = "ListMetadata", description = "Metadata for list responses")
public class ListMetadata extends BaseMetadata {
    
    @Schema(description = "Total number of items in the list", example = "25")
    private Long totalCount;
    
    @Schema(description = "Number of items returned in this response", example = "25")
    private Integer count;
    
    @Schema(description = "Whether the list is filtered", example = "false")
    private Boolean filtered;
    
    @Schema(description = "Applied filters description", example = "status=active")
    private String filterDescription;
    
    public static ListMetadata forList(List<?> list) {
        return ListMetadata.builder()
                .dataType("list")
                .count(list != null ? list.size() : 0)
                .totalCount(list != null ? (long) list.size() : 0L)
                .filtered(false)
                .build();
    }
    
    public static ListMetadata forList(List<?> list, String filterDescription) {
        return ListMetadata.builder()
                .dataType("list")
                .count(list != null ? list.size() : 0)
                .totalCount(list != null ? (long) list.size() : 0L)
                .filtered(filterDescription != null && !filterDescription.trim().isEmpty())
                .filterDescription(filterDescription)
                .build();
    }
    
    public static ListMetadata forList(List<?> list, boolean filtered, String filterDescription) {
        return ListMetadata.builder()
                .dataType("list")
                .count(list != null ? list.size() : 0)
                .totalCount(list != null ? (long) list.size() : 0L)
                .filtered(filtered)
                .filterDescription(filterDescription)
                .build();
    }
    
    public static ListMetadata forList(List<?> list, String filterDescription, Object additional) {
        return ListMetadata.builder()
                .dataType("list")
                .count(list != null ? list.size() : 0)
                .totalCount(list != null ? (long) list.size() : 0L)
                .filtered(filterDescription != null && !filterDescription.trim().isEmpty())
                .filterDescription(filterDescription)
                .additional(additional)
                .build();
    }
}
