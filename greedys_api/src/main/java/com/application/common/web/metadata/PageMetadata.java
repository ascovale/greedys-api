package com.application.common.web.metadata;

import org.springframework.data.domain.Page;

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
@Schema(name = "PageMetadata", description = "Metadata for paginated responses")
public class PageMetadata extends BaseMetadata {
    
    @Schema(description = "Total number of items across all pages", example = "150")
    private Long totalCount;
    
    @Schema(description = "Number of items in current page", example = "20")
    private Integer count;
    
    @Schema(description = "Current page number (0-based)", example = "0")
    private Integer page;
    
    @Schema(description = "Number of items per page", example = "20")
    private Integer size;
    
    @Schema(description = "Total number of pages", example = "8")
    private Integer totalPages;
    
    @Schema(description = "Whether this is the first page", example = "true")
    private Boolean first;
    
    @Schema(description = "Whether this is the last page", example = "false")
    private Boolean last;
    
    @Schema(description = "Number of elements in current page", example = "20")
    private Integer numberOfElements;
    
    @Schema(description = "Whether the list is filtered", example = "false")
    private Boolean filtered;
    
    @Schema(description = "Applied filters description", example = "status=active")
    private String filterDescription;
    
    public static <T> PageMetadata forPage(Page<T> page) {
        return PageMetadata.builder()
                .dataType("page")
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalCount(page.getTotalElements())
                .count(page.getNumberOfElements())
                .numberOfElements(page.getNumberOfElements())
                .first(page.isFirst())
                .last(page.isLast())
                .filtered(false)
                .build();
    }
    
    public static <T> PageMetadata forPage(Page<T> page, String filterDescription) {
        return PageMetadata.builder()
                .dataType("page")
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalCount(page.getTotalElements())
                .count(page.getNumberOfElements())
                .numberOfElements(page.getNumberOfElements())
                .first(page.isFirst())
                .last(page.isLast())
                .filtered(filterDescription != null && !filterDescription.trim().isEmpty())
                .filterDescription(filterDescription)
                .build();
    }
    
    public static <T> PageMetadata forPage(Page<T> page, boolean filtered, String filterDescription) {
        return PageMetadata.builder()
                .dataType("page")
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalCount(page.getTotalElements())
                .count(page.getNumberOfElements())
                .numberOfElements(page.getNumberOfElements())
                .first(page.isFirst())
                .last(page.isLast())
                .filtered(filtered)
                .filterDescription(filterDescription)
                .build();
    }
    
    public static <T> PageMetadata forPage(Page<T> page, String filterDescription, Object additional) {
        return PageMetadata.builder()
                .dataType("page")
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalCount(page.getTotalElements())
                .count(page.getNumberOfElements())
                .numberOfElements(page.getNumberOfElements())
                .first(page.isFirst())
                .last(page.isLast())
                .filtered(filterDescription != null && !filterDescription.trim().isEmpty())
                .filterDescription(filterDescription)
                .additional(additional)
                .build();
    }
}
