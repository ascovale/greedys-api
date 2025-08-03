package com.application.common.web;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "PageResponseWrapper", description = "Standard API response wrapper for paginated data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponseWrapper<T> {

    @Schema(description = "Indicates if the request was successful", example = "true")
    @Builder.Default
    private boolean success = true;

    @Schema(description = "Response message", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Response data as page")
    private Page<T> data;

    @Schema(description = "Error details (present only if success = false)")
    private ResponseWrapper.ErrorDetails error;

    @Schema(description = "Response timestamp")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Schema(description = "Page metadata (pagination info)")
    private PageMetadata metadata;

    // Success methods
    public static <T> PageResponseWrapper<T> success(Page<T> data) {
        return PageResponseWrapper.<T>builder()
                .success(true)
                .data(data)
                .message("Operation completed successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> PageResponseWrapper<T> success(Page<T> data, String message) {
        return PageResponseWrapper.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> PageResponseWrapper<T> success(Page<T> data, String message, PageMetadata metadata) {
        return PageResponseWrapper.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .metadata(metadata)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Error methods
    public static <T> PageResponseWrapper<T> error(String message) {
        return PageResponseWrapper.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> PageResponseWrapper<T> error(String message, ResponseWrapper.ErrorDetails error) {
        return PageResponseWrapper.<T>builder()
                .success(false)
                .message(message)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PageMetadata {
        @Schema(description = "Current page number (0-based)", example = "0")
        private Integer page;
        
        @Schema(description = "Number of items per page", example = "20")
        private Integer size;
        
        @Schema(description = "Total number of items", example = "150")
        private Long totalElements;
        
        @Schema(description = "Total number of pages", example = "8")
        private Integer totalPages;
        
        @Schema(description = "Whether this is the first page", example = "true")
        private Boolean first;
        
        @Schema(description = "Whether this is the last page", example = "false")
        private Boolean last;
        
        @Schema(description = "Number of elements in current page", example = "20")
        private Integer numberOfElements;
        
        @Schema(description = "Additional pagination metadata")
        private Object additional;
    }
}
