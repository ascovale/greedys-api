package com.application.common.devtools.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark controllers or individual endpoints as not active.
 * 
 * When applied to a controller class, all endpoints in that controller are disabled.
 * When applied to a method, only that specific endpoint is disabled.
 * 
 * Disabled endpoints:
 * - Return 404 Not Found when called
 * - Are hidden from Swagger/OpenAPI documentation
 * 
 * Usage examples:
 * <pre>
 * // Disable entire controller
 * {@literal @}NotActive
 * {@literal @}RestController
 * public class MyController { ... }
 * 
 * // Disable single endpoint
 * {@literal @}RestController
 * public class MyController {
 *     {@literal @}NotActive
 *     {@literal @}GetMapping("/disabled")
 *     public String disabledEndpoint() { ... }
 *     
 *     {@literal @}GetMapping("/active")
 *     public String activeEndpoint() { ... }
 * }
 * 
 * // With reason
 * {@literal @}NotActive(reason = "Under development - Issue #123")
 * {@literal @}GetMapping("/wip")
 * public String wipEndpoint() { ... }
 * </pre>
 * 
 * @author Greedys Development Team
 * @since 1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NotActive {
    
    /**
     * Optional reason why this endpoint/controller is not active.
     * Useful for documentation and debugging purposes.
     * 
     * @return the reason for deactivation
     */
    String reason() default "";
    
    /**
     * Optional version when this endpoint/controller will be activated.
     * 
     * @return the target version for activation
     */
    String targetVersion() default "";
    
    /**
     * Whether to log access attempts to disabled endpoints.
     * Default is true.
     * 
     * @return true if access attempts should be logged
     */
    boolean logAttempts() default true;
}
