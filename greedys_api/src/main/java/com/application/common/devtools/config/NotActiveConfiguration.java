package com.application.common.devtools.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.application.common.devtools.interceptor.NotActiveInterceptor;

import lombok.RequiredArgsConstructor;

/**
 * Configuration class that registers the NotActiveInterceptor.
 * 
 * This configuration ensures that all requests are checked for @NotActive
 * annotations before being processed by the controller.
 * 
 * @author Greedys Development Team
 * @since 1.0
 */
@Configuration
@RequiredArgsConstructor
public class NotActiveConfiguration implements WebMvcConfigurer {

    private final NotActiveInterceptor notActiveInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(notActiveInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/actuator/**",
                    "/error"
                );
    }
}
