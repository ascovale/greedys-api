package com.application.common.controller.validators;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates that a city name is valid for the specified country
 */
@Documented
@Constraint(validatedBy = CityValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCity {
    
    String message() default "{ValidCity.message}";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether null values are allowed
     */
    boolean allowNull() default false;
    
    /**
     * Country code for which to validate cities (IT, US, FR, etc.)
     * If empty, will try to detect from context or allow any reasonable city name
     */
    String country() default "";
    
    /**
     * Whether to perform strict validation (only predefined cities)
     * or lenient validation (reasonable city name pattern)
     */
    boolean strictValidation() default false;
}
