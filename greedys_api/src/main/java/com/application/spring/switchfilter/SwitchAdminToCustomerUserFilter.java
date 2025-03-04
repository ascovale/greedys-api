package com.application.spring.switchfilter;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;

public class SwitchAdminToCustomerUserFilter extends SwitchUserFilter {

    public SwitchAdminToCustomerUserFilter(UserDetailsService customerUserDetailsService) {
        setUserDetailsService(customerUserDetailsService);
        setSwitchUserUrl("/admin/switchToCustomerUser");
        setExitUserUrl("/admin/exitCustomerUser");
        setTargetUrl("/admin/home");
    }
}