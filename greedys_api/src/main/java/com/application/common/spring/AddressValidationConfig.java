package com.application.common.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "address.validation")
@Data
public class AddressValidationConfig {
    
    private boolean enabled = true;
    private int timeout = 5000;
    private int maxRetries = 3;
}
