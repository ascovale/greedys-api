package com.application.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "geocoding")
@Data
public class GeocodingConfig {
    
    private Google google = new Google();
    private Nominatim nominatim = new Nominatim();
    
    @Data
    public static class Google {
        private String apiKey;
        private String baseUrl;
    }
    
    @Data
    public static class Nominatim {
        private String baseUrl;
        private String userAgent;
    }
}
