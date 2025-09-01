package com.application.common.web;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;

import com.application.common.web.metadata.BaseMetadata;
import com.application.common.web.metadata.ListMetadata;
import com.application.common.web.metadata.PageMetadata;
import com.application.common.web.metadata.SingleMetadata;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "ResponseWrapper", description = "Standard API response wrapper")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseWrapper<T> {

    @Schema(description = "Response message", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Response data")    
    private T data;

    @Schema(description = "Response timestamp")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Schema(description = "Response metadata (structure varies by data type)")
    private BaseMetadata metadata;

    // Success responses
    public static <T> ResponseWrapper<T> success(T data) {
        return ResponseWrapper.<T>builder()
                .data(data)
                .message("Success")
                .metadata(SingleMetadata.create())
                .build();
    }

    public static <T> ResponseWrapper<T> success(T data, String message) {
        return ResponseWrapper.<T>builder()
                .data(data)
                .message(message)
                .metadata(SingleMetadata.create())
                .build();
    }

    public static <T> ResponseWrapper<T> success(T data, String message, BaseMetadata metadata) {
        return ResponseWrapper.<T>builder()
                .data(data)
                .message(message)
                .metadata(metadata)
                .build();
    }

    // List success responses
    public static <T> ResponseWrapper<List<T>> successList(List<T> data) {
        return ResponseWrapper.<List<T>>builder()
                .data(data)
                .message("Operation completed successfully")
                .metadata(ListMetadata.forList(data))
                .build();
    }

    public static <T> ResponseWrapper<List<T>> successList(List<T> data, String message) {
        return ResponseWrapper.<List<T>>builder()
                .data(data)
                .message(message)
                .metadata(ListMetadata.forList(data))
                .build();
    }

    public static <T> ResponseWrapper<List<T>> successList(List<T> data, String message, String filterDescription) {
        return ResponseWrapper.<List<T>>builder()
                .data(data)
                .message(message)
                .metadata(ListMetadata.forList(data, filterDescription))
                .build();
    }

    // Page success responses
    public static <T> ResponseWrapper<Page<T>> successPage(Page<T> page) {
        return ResponseWrapper.<Page<T>>builder()
                .data(page)
                .message(String.format("Page %d of %d (%d total items)", 
                        page.getNumber() + 1, page.getTotalPages(), page.getTotalElements()))
                .metadata(PageMetadata.forPage(page))
                .build();
    }

    public static <T> ResponseWrapper<Page<T>> successPage(Page<T> page, String message) {
        return ResponseWrapper.<Page<T>>builder()
                .data(page)
                .message(message)
                .metadata(PageMetadata.forPage(page))
                .build();
    }

    public static <T> ResponseWrapper<List<T>> successPage(Page<T> page, String message, String filterDescription) {
        return ResponseWrapper.<List<T>>builder()
                .data(page.getContent())
                .message(message)
                .metadata(PageMetadata.forPage(page, filterDescription))
                .build();
    }

    // Created response (201)
    public static <T> ResponseWrapper<T> created(T data, String message) {
        return ResponseWrapper.<T>builder()
                .data(data)
                .message(message)
                .metadata(SingleMetadata.create())
                .build();
    }

    // Created response for lists (201)
    public static <T> ResponseWrapper<List<T>> createdList(List<T> data, String message) {
        return ResponseWrapper.<List<T>>builder()
                .data(data)
                .message(message)
                .metadata(ListMetadata.forList(data))
                .build();
    }

    // Error responses
        public static ResponseWrapper<ErrorDetails> error(String message) {
                return ResponseWrapper.<ErrorDetails>builder()
                                .message(message)
                                .data(ErrorDetails.builder().build())
                                .build();
        }

        public static ResponseWrapper<ErrorDetails> badRequest(String message, String errorCode) {
                return ResponseWrapper.<ErrorDetails>builder()
                                .message(message)
                                .data(ErrorDetails.builder().code(errorCode).build())
                                .build();
        }

        public static ResponseWrapper<ErrorDetails> notFound(String message) {
                return ResponseWrapper.<ErrorDetails>builder()
                                .message(message)
                                .data(ErrorDetails.builder().code("NOT_FOUND").build())
                                .build();
        }

        public static ResponseWrapper<ErrorDetails> conflict(String message) {
                return ResponseWrapper.<ErrorDetails>builder()
                                .message(message)
                                .data(ErrorDetails.builder().code("CONFLICT").build())
                                .build();
        }

        public static ResponseWrapper<ErrorDetails> unauthorized(String message) {
                return ResponseWrapper.<ErrorDetails>builder()
                                .message(message)
                                .data(ErrorDetails.builder().code("UNAUTHORIZED").build())
                                .build();
        }

        public static ResponseWrapper<ErrorDetails> forbidden(String message) {
                return ResponseWrapper.<ErrorDetails>builder()
                                .message(message)
                                .data(ErrorDetails.builder().code("FORBIDDEN").build())
                                .build();
        }

        public static ResponseWrapper<ErrorDetails> internalServerError(String message) {
                return ResponseWrapper.<ErrorDetails>builder()
                                .message(message)
                                .data(ErrorDetails.builder().code("INTERNAL_SERVER_ERROR").build())
                                .build();
        }

    // Error responses with custom status codes
        public static ResponseWrapper<ErrorDetails> errorWithStatus(int statusCode, String message, String errorCode) {
                return ResponseWrapper.<ErrorDetails>builder()
                                .message(message)
                                .data(ErrorDetails.builder().code(errorCode).build())
                                .build();
        }

        public static ResponseWrapper<ErrorDetails> error(String message, String code) {
                return ResponseWrapper.<ErrorDetails>builder()
                                .message(message)
                                .data(ErrorDetails.builder().code(code).build())
                                .build();
        }

        public static ResponseWrapper<ErrorDetails> error(ErrorDetails error) {
                return ResponseWrapper.<ErrorDetails>builder()
                                .message("An error occurred")  // Messaggio di fallback se non specificato
                                .data(error)
                                .build();
        }

}
