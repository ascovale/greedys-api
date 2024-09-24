package com.application.controller.Validators;

import com.application.web.dto.post.NewUserDTO;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

	@Override
    public void initialize(PasswordMatches constraintAnnotation) {
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        NewUserDTO user = (NewUserDTO) obj;
        return user.getPassword().equals(user.getMatchingPassword());
    }

}
