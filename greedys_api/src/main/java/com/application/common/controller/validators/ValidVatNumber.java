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
 * Annotation for validating VAT/Tax numbers according to international standards.
 * 
 * Supports multiple regions worldwide:
 * - European Union (all 27 countries) - VIES format
 * - European non-EU countries (UK, Switzerland, Norway, etc.)
 * - North America (USA EIN, Canada GST/HST, Mexico RFC)
 * - Asia-Pacific (Australia ABN, Japan, Singapore UEN, India GSTIN, etc.)
 * - South America (Brazil CNPJ, Argentina CUIT, Chile RUT, etc.)
 * - Africa (South Africa, Egypt, Morocco, etc.)
 * - Middle East (UAE TRN, Saudi Arabia, Israel, etc.)
 * 
 * Examples of valid formats:
 * - European: IT12345678901, FR12345678901, DE123456789
 * - North America: US12-3456789, CA123456789RT0001, MXABC123456DEF
 * - Asia-Pacific: AU12345678901, SG12345678A, IN22AAAAA0000A1Z5
 * - South America: BR12.345.678/0001-90, CL12345678-9
 * - And many more...
 * 
 * @author Generated for Greedys API
 */
@Documented
@Constraint(validatedBy = VatNumberValidator.class)
@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
@Retention(RUNTIME)
public @interface ValidVatNumber {
    String message() default "{ValidVatNumber.message}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to allow null values. If true, null values will pass validation.
     */
    boolean allowNull() default true;
}
