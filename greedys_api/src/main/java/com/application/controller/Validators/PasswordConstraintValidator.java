package com.application.controller.Validators;

import java.util.Arrays;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.passay.WhitespaceRule;

import com.google.common.base.Joiner;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public void initialize(final ValidPassword arg0) {

    }

    @Override
    public boolean isValid(final String password, final ConstraintValidatorContext context) {
        PasswordValidator validator = new PasswordValidator(Arrays.asList(
                // Lunghezza minima 8 caratteri, massima 16
                new LengthRule(8, 16),

                // Almeno un carattere minuscolo
                new CharacterRule(EnglishCharacterData.LowerCase, 1),

                // Almeno un carattere maiuscolo
                new CharacterRule(EnglishCharacterData.UpperCase, 1),

                // Almeno un numero
                new CharacterRule(EnglishCharacterData.Digit, 1),

                // Almeno un carattere speciale
                new CharacterRule(EnglishCharacterData.Special, 1),

                // Nessuno spazio bianco
                new WhitespaceRule()
        ));


        // Crea un validatore di password con la regola di caratteristiche di caratteri
        RuleResult result = validator.validate(new PasswordData(password));

        if (result.isValid()) {
            return true;
        } else {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    Joiner.on(",").join(validator.getMessages(result)))
                    .addConstraintViolation();
            return false;
        }
    }

}
