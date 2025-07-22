package com.application.common.controller.validators;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Annotation for validating physical addresses.
 * 
 * This validator performs multiple checks:
 * - Basic format validation (minimum length, required components)
 * - Pattern matching for common address formats
 * - Optional integration with geocoding services for real address verification
 * 
 * Examples of valid addresses:
 * - "Via Roma 123, 20121 Milano, Italy"
 * - "123 Main Street, New York, NY 10001, USA"
 * - "1 rue de la Paix, 75001 Paris, France"
 * 
 * @author Generated for Greedys API
 */
@Documented
@Constraint(validatedBy = AddressValidator.class)
@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
@Retention(RUNTIME)
public @interface ValidAddress {
    String message() default "{ValidAddress.message}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to allow null values. If true, null values will pass validation.
     */
    boolean allowNull() default true;
    
    /**
     * Whether to perform strict validation including geocoding verification.
     * If true, the address will be validated against external geocoding services.
     * If false, only basic format validation will be performed.
     */
    boolean strictValidation() default false;
    
    /**
     * Minimum length for the address string.
     */
    int minLength() default 10;
    
    /**
     * Maximum length for the address string.
     */
    int maxLength() default 500;
}
