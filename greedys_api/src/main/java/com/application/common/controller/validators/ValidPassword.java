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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Documented
@Constraint(validatedBy = {})  // nessun validator custom
@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
@Retention(RUNTIME)
// Lunghezza tra 8 e 16
@Size(min = 8, max = 16,
    message = "La password deve essere lunga tra {min} e {max} caratteri")
// Almeno un minuscolo, un maiuscolo, un numero, un carattere speciale e nessuno spazio
@Pattern(
    regexp = "^(?=\\S+$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*\\W).+$",
    message = "Deve contenere almeno un carattere minuscolo, uno maiuscolo, un numero, un carattere speciale e nessuno spazio"
)
public @interface ValidPassword {
    String message() default "Password non valida";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
