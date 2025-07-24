package com.application.common.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Mapping per logo custom
        registry.addResourceHandler("/logo_api.png")
                .addResourceLocations("classpath:/static/logo_api.png");
        // Mapping per favicon
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico");
        // Regola per servire la custom Swagger UI con priorità sulla cartella custom
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/static/swagger-ui/", "classpath:/META-INF/resources/webjars/");
    }
}
