package com.application.registration;

import java.util.Locale;

import org.springframework.context.ApplicationEvent;

import com.application.persistence.model.user.User;

@SuppressWarnings("serial")
public class UserOnRegistrationCompleteEvent extends ApplicationEvent {

    private final String appUrl;
    private final Locale locale;
    private final User user;

    public UserOnRegistrationCompleteEvent(final User registered, final Locale locale, final String appUrl) {
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

    public User getUser() {
        return user;
    }

}
