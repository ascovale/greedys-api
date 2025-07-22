package com.application.customer;

import java.util.Locale;

import org.springframework.context.ApplicationEvent;

import com.application.customer.model.Customer;

public class CustomerOnRegistrationCompleteEvent extends ApplicationEvent {

    private final String appUrl;
    private final Locale locale;
    private final Customer user;

    public CustomerOnRegistrationCompleteEvent(final Customer registered, final Locale locale, final String appUrl) {
        super(registered);
        this.user = registered;
        this.locale = locale;
        this.appUrl = appUrl;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public Locale getLocale() {
        return locale;
    }

    public Customer getUser() {
        return user;
    }

}
