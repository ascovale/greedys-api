package com.application.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "geocoding")
public class GeocodingConfig {
    
    private Google google = new Google();
    private Nominatim nominatim = new Nominatim();
    
    public Google getGoogle() {
        return google;
    }
    
    public void setGoogle(Google google) {
        this.google = google;
    }
    
    public Nominatim getNominatim() {
        return nominatim;
    }
    
    public void setNominatim(Nominatim nominatim) {
        this.nominatim = nominatim;
    }
    
    public static class Google {
        private String apiKey;
        private String baseUrl;
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
    
    public static class Nominatim {
        private String baseUrl;
        private String userAgent;
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        public String getUserAgent() {
            return userAgent;
        }
        
        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }
    }
}
