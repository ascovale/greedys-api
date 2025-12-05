package com.application.common.devtools.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.devtools.annotation.NotActive;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Example controller demonstrating @NotActive annotation usage.
 * 
 * This controller shows different ways to use @NotActive:
 * 1. On individual methods to disable specific endpoints
 * 2. The entire controller would be disabled if @NotActive was on the class
 * 
 * NOTE: This is an example controller for documentation purposes.
 *       Delete or disable this in production.
 */
@NotActive(reason = "Example controller - not for production use")
@Tag(name = "NotActive Example", description = "Example endpoints demonstrating @NotActive usage")
@RestController
@RequestMapping("/example/notactive")
public class NotActiveExampleController {

    /**
     * This endpoint is active and will appear in Swagger.
     */
    @Operation(summary = "Active endpoint", description = "This endpoint is active and functional")
    @GetMapping("/active")
    public ResponseEntity<String> activeEndpoint() {
        return ResponseEntity.ok("This endpoint is active!");
    }

    /**
     * This endpoint is disabled at method level.
     * It will return 404 and won't appear in Swagger.
     */
    @NotActive(reason = "Under development", targetVersion = "2.0")
    @Operation(summary = "Disabled endpoint", description = "This won't be visible in Swagger")
    @GetMapping("/disabled")
    public ResponseEntity<String> disabledEndpoint() {
        // This code will never execute
        return ResponseEntity.ok("You shouldn't see this!");
    }

    /**
     * Another disabled endpoint without logging.
     */
    @NotActive(reason = "Deprecated - use /v2/feature instead", logAttempts = false)
    @Operation(summary = "Silent disabled", description = "Disabled without logging attempts")
    @PostMapping("/silent-disabled")
    public ResponseEntity<String> silentDisabledEndpoint() {
        return ResponseEntity.ok("Silent disabled");
    }
}
