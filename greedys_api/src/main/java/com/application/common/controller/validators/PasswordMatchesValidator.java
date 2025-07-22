package com.application.common.controller.validators;

import com.application.admin.web.post.NewAdminDTO;
import com.application.customer.web.post.NewCustomerDTO;
import com.application.restaurant.web.post.NewRUserDTO;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

	@Override
    public void initialize(PasswordMatches constraintAnnotation) {
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj instanceof NewCustomerDTO) {
            NewCustomerDTO user = (NewCustomerDTO) obj;
            return user.getPassword().equals(user.getMatchingPassword());
        } else if (obj instanceof NewAdminDTO) {
            NewAdminDTO admin = (NewAdminDTO) obj;
            return admin.getPassword().equals(admin.getMatchingPassword());
        } else if (obj instanceof NewRUserDTO) {
            NewRUserDTO RUser = (NewRUserDTO) obj;
            return RUser.getPassword().equals(RUser.getMatchingPassword());
        }
            throw new IllegalArgumentException("Unsupported object type");
        
    }


}
