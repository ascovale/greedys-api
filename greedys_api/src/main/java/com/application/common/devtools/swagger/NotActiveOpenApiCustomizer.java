package com.application.common.devtools.swagger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.devtools.annotation.NotActive;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OpenAPI customizer that removes entire controllers marked with @NotActive from Swagger.
 * 
 * While NotActiveOperationCustomizer handles individual operations, this customizer
 * ensures that paths from disabled controllers are completely removed from the
 * OpenAPI specification.
 * 
 * @author Greedys Development Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotActiveOpenApiCustomizer implements OpenApiCustomizer {

    private final ApplicationContext applicationContext;

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }

        List<String> pathsToRemove = new ArrayList<>();
        
        // Find all controllers marked with @NotActive
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(RestController.class);
        
        for (Object controller : controllers.values()) {
            Class<?> controllerClass = controller.getClass();
            
            // Handle Spring proxies
            if (controllerClass.getName().contains("$$")) {
                controllerClass = controllerClass.getSuperclass();
            }
            
            if (controllerClass.isAnnotationPresent(NotActive.class)) {
                // Get the base path of this controller
                String basePath = getControllerBasePath(controllerClass);
                
                if (basePath != null) {
                    // Mark all paths starting with this base path for removal
                    for (String path : openApi.getPaths().keySet()) {
                        if (path.startsWith(basePath) || path.equals(basePath)) {
                            pathsToRemove.add(path);
                        }
                    }
                    
                    NotActive annotation = controllerClass.getAnnotation(NotActive.class);
                    log.debug("Hiding controller from Swagger: {} (Reason: {})", 
                            controllerClass.getSimpleName(), 
                            annotation.reason().isEmpty() ? "Not specified" : annotation.reason());
                }
            }
        }
        
        // Remove the marked paths
        for (String path : pathsToRemove) {
            openApi.getPaths().remove(path);
            log.trace("Removed path from OpenAPI: {}", path);
        }
        
        // Clean up empty tags
        if (openApi.getTags() != null) {
            openApi.getTags().removeIf(tag -> {
                // Check if any path still uses this tag
                for (PathItem pathItem : openApi.getPaths().values()) {
                    if (pathItemUsesTag(pathItem, tag.getName())) {
                        return false;
                    }
                }
                return true;
            });
        }
    }
    
    private String getControllerBasePath(Class<?> controllerClass) {
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        if (requestMapping != null) {
            String[] paths = requestMapping.value().length > 0 ? 
                    requestMapping.value() : requestMapping.path();
            if (paths.length > 0) {
                return paths[0];
            }
        }
        return null;
    }
    
    private boolean pathItemUsesTag(PathItem pathItem, String tagName) {
        return checkOperationTag(pathItem.getGet(), tagName) ||
               checkOperationTag(pathItem.getPost(), tagName) ||
               checkOperationTag(pathItem.getPut(), tagName) ||
               checkOperationTag(pathItem.getDelete(), tagName) ||
               checkOperationTag(pathItem.getPatch(), tagName) ||
               checkOperationTag(pathItem.getOptions(), tagName) ||
               checkOperationTag(pathItem.getHead(), tagName);
    }
    
    private boolean checkOperationTag(io.swagger.v3.oas.models.Operation operation, String tagName) {
        return operation != null && 
               operation.getTags() != null && 
               operation.getTags().contains(tagName);
    }
}
