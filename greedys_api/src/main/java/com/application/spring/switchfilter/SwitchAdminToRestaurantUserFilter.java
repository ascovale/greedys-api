package com.application.spring.switchfilter;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;

public class SwitchAdminToRestaurantUserFilter extends SwitchUserFilter {

    public SwitchAdminToRestaurantUserFilter(UserDetailsService restaurantUserDetailsService) {
        setUserDetailsService(restaurantUserDetailsService);
        setSwitchUserUrl("/admin/switchToRestaurantUser");
        setExitUserUrl("/admin/exitRestaurantUser");
        setTargetUrl("/admin/home");
    }
}