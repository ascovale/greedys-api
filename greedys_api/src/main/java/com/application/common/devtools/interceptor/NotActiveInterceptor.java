package com.application.common.devtools.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.application.common.devtools.annotation.NotActive;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Interceptor that checks for @NotActive annotation on controllers and methods.
 * 
 * When an endpoint marked with @NotActive is called:
 * - Returns 404 Not Found
 * - Optionally logs the access attempt
 * 
 * This interceptor is automatically registered by NotActiveConfiguration.
 * 
 * @author Greedys Development Team
 * @since 1.0
 */
@Slf4j
@Component
public class NotActiveInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // Check method-level annotation first (more specific)
        NotActive methodAnnotation = handlerMethod.getMethodAnnotation(NotActive.class);
        if (methodAnnotation != null) {
            handleNotActiveEndpoint(request, response, methodAnnotation, 
                    handlerMethod.getMethod().getName());
            return false;
        }
        
        // Check class-level annotation
        NotActive classAnnotation = handlerMethod.getBeanType().getAnnotation(NotActive.class);
        if (classAnnotation != null) {
            handleNotActiveEndpoint(request, response, classAnnotation, 
                    handlerMethod.getBeanType().getSimpleName());
            return false;
        }
        
        return true;
    }
    
    private void handleNotActiveEndpoint(HttpServletRequest request, 
                                         HttpServletResponse response,
                                         NotActive annotation, 
                                         String targetName) throws Exception {
        
        if (annotation.logAttempts()) {
            String reasonInfo = annotation.reason().isEmpty() ? "" : " (Reason: " + annotation.reason() + ")";
            String versionInfo = annotation.targetVersion().isEmpty() ? "" : " [Target: " + annotation.targetVersion() + "]";
            
            log.warn("Access attempt to disabled endpoint: {} {} - Target: {}{}{}",
                    request.getMethod(),
                    request.getRequestURI(),
                    targetName,
                    reasonInfo,
                    versionInfo);
        }
        
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format(
            "{\"error\":\"Not Found\",\"message\":\"The requested endpoint is not available\",\"status\":404,\"path\":\"%s\"}",
            request.getRequestURI()
        );
        
        response.getWriter().write(jsonResponse);
    }
}
