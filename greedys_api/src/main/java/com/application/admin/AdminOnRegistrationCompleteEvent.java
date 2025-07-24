package com.application.admin;

import java.util.Locale;

import org.springframework.context.ApplicationEvent;

import com.application.admin.persistence.model.Admin;

import lombok.Getter;

@Getter
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

}
