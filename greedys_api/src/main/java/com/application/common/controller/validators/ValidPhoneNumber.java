package com.application.common.controller.validators;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates phone number format for international and national formats
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhoneNumber {
    
    String message() default "{ValidPhoneNumber.message}";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether null values are allowed
     */
    boolean allowNull() default true;
    
    /**
     * Country code for which to validate phone numbers (IT, US, FR, etc.)
     * If empty, will accept international format
     */
    String country() default "";
    
    /**
     * Whether to require international format (+39, +1, etc.)
     */
    boolean requireInternational() default false;
}
