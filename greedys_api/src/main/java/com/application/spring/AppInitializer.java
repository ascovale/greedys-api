package com.application.spring;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppInitializer {

    @Autowired
    private TwilioConfig twilioConfig;

    @PostConstruct
    public void init() {
        twilioConfig.init();
    }
}