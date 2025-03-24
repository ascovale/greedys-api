package com.application.registration;

import java.util.Locale;

import org.springframework.context.ApplicationEvent;

import com.application.persistence.model.admin.Admin;

public class AdminOnRegistrationCompleteEvent extends ApplicationEvent {

    private final String appUrl;
    private final Locale locale;
    private final Admin admin;

    public AdminOnRegistrationCompleteEvent(final Admin registered, final Locale locale, final String appUrl) {
        super(registered);
        this.admin = registered;
        this.locale = locale;
        this.appUrl = appUrl;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public Locale getLocale() {
        return locale;
    }

    public Admin getAdmin() {
        return admin;
    }

}
