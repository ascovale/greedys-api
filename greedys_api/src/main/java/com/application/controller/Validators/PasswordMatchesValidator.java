package com.application.controller.Validators;

import com.application.web.dto.post.NewCustomerDTO;
import com.application.web.dto.post.NewRestaurantUserDTO;
import com.application.web.dto.post.admin.NewAdminDTO;

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
        } else if (obj instanceof NewRestaurantUserDTO) {
            NewRestaurantUserDTO restaurantUser = (NewRestaurantUserDTO) obj;
            return restaurantUser.getPassword().equals(restaurantUser.getMatchingPassword());
        }
            throw new IllegalArgumentException("Unsupported object type");
        
    }


}
