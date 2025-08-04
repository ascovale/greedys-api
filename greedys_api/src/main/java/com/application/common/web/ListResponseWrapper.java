package com.application.common.web;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "ListResponseWrapper", description = "Standard API response wrapper for list data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListResponseWrapper<T> {

    @Schema(description = "Indicates if the request was successful", example = "true")
    @Builder.Default
    private boolean success = true;

    @Schema(description = "Response message", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Response data as list")
    private List<T> data;

    @Schema(description = "Error details (present only if success = false)")
    private ErrorDetails error;

    @Schema(description = "Response timestamp")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Schema(description = "List metadata (counts, etc.)")
    private ListMetadata metadata;

    // Success methods
    public static <T> ListResponseWrapper<T> success(List<T> data) {
        return ListResponseWrapper.<T>builder()
                .success(true)
                .data(data)
                .message("Operation completed successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ListResponseWrapper<T> success(List<T> data, String message) {
        return ListResponseWrapper.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ListResponseWrapper<T> success(List<T> data, String message, ListMetadata metadata) {
        return ListResponseWrapper.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .metadata(metadata)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Error methods
    public static <T> ListResponseWrapper<T> error(String message) {
        return ListResponseWrapper.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ListResponseWrapper<T> error(String message, ErrorDetails error) {
        return ListResponseWrapper.<T>builder()
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
    public static class ListMetadata {
        @Schema(description = "Total number of items in the list", example = "25")
        private Long totalCount;
        
        @Schema(description = "Number of items returned in this response", example = "25")
        private Integer count;
        
        @Schema(description = "Whether the list is filtered", example = "true")
        private Boolean filtered;
        
        @Schema(description = "Applied filters description", example = "status=active")
        private String filterDescription;
        
        @Schema(description = "Additional list metadata")
        private Object additional;
    }
}
