package com.application.common.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.application.admin.controller.admin.AdminAuthenticationController;
import com.application.customer.controller.customer.CustomerAuthenticationController;
import com.application.restaurant.controller.RestaurantAuthenticationController;

/**
 * Test che verifica che gli endpoint di autenticazione siano configurati correttamente
 * e corrispondano a quelli definiti nel TokenTypeValidationFilter.
 * 
 * ⚠️ IMPORTANTE: Questo test fallirà se vengono modificati i percorsi degli endpoint
 * di refresh nei controller senza aggiornare il TokenTypeValidationFilter.
 */
class AuthenticationEndpointValidationTest {

    @Test
    void shouldValidateRefreshEndpointsExistAndMatchFilter() {
        // 🎯 Endpoint di refresh che DEVONO esistere come definito nel TokenTypeValidationFilter
        Set<String> expectedRefreshEndpoints = Set.of(
            "/customer/auth/refresh",
            "/admin/auth/refresh", 
            "/restaurant/user/auth/refresh",
            "/restaurant/user/auth/refresh/hub"
        );
        
        // 🔍 Verifica CustomerAuthenticationController
        String customerBasePath = getRequestMappingPath(CustomerAuthenticationController.class);
        Set<String> customerRefreshPaths = getRefreshEndpoints(CustomerAuthenticationController.class, customerBasePath);
        
        // 🔍 Verifica AdminAuthenticationController  
        String adminBasePath = getRequestMappingPath(AdminAuthenticationController.class);
        Set<String> adminRefreshPaths = getRefreshEndpoints(AdminAuthenticationController.class, adminBasePath);
        
        // 🔍 Verifica RestaurantAuthenticationController
        String restaurantBasePath = getRequestMappingPath(RestaurantAuthenticationController.class);
        Set<String> restaurantRefreshPaths = getRefreshEndpoints(RestaurantAuthenticationController.class, restaurantBasePath);
        
        // 📋 Raccogli tutti gli endpoint effettivi
        Set<String> actualRefreshEndpoints = Set.of();
        actualRefreshEndpoints = java.util.stream.Stream.of(
            customerRefreshPaths, 
            adminRefreshPaths, 
            restaurantRefreshPaths
        ).flatMap(Set::stream).collect(Collectors.toSet());
        
        // ✅ Verifica che tutti gli endpoint attesi esistano
        for (String expectedEndpoint : expectedRefreshEndpoints) {
            if (!actualRefreshEndpoints.contains(expectedEndpoint)) {
                fail(String.format(
                    "❌ ENDPOINT MANCANTE: '%s' è definito nel TokenTypeValidationFilter.isRefreshEndpoint() " +
                    "ma non esiste nei controller!\n" +
                    "🔧 AZIONE RICHIESTA: Aggiorna TokenTypeValidationFilter.isRefreshEndpoint() " +
                    "per rimuovere questo endpoint o aggiungilo al controller appropriato.\n" +
                    "📍 Endpoint effettivi trovati: %s", 
                    expectedEndpoint, actualRefreshEndpoints
                ));
            }
        }
        
        // ⚠️ Verifica che non ci siano endpoint extra non gestiti
        for (String actualEndpoint : actualRefreshEndpoints) {
            if (!expectedRefreshEndpoints.contains(actualEndpoint)) {
                fail(String.format(
                    "❌ ENDPOINT NON GESTITO: '%s' esiste nel controller " +
                    "ma NON è definito nel TokenTypeValidationFilter.isRefreshEndpoint()!\n" +
                    "🔧 AZIONE RICHIESTA: Aggiungi questo endpoint al metodo TokenTypeValidationFilter.isRefreshEndpoint() " +
                    "se è un endpoint di refresh, oppure rinominalo se non dovrebbe essere di refresh.\n" +
                    "📍 Endpoint attesi nel filtro: %s", 
                    actualEndpoint, expectedRefreshEndpoints
                ));
            }
        }
        
        // ✅ Test passato!
        System.out.println("✅ VALIDAZIONE COMPLETATA: Tutti gli endpoint di refresh sono sincronizzati tra controller e filtro");
        System.out.println("📋 Endpoint di refresh validati: " + expectedRefreshEndpoints);
    }
    
    /**
     * Estrae il percorso base dal @RequestMapping del controller
     */
    private String getRequestMappingPath(Class<?> controllerClass) {
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping, 
            "Controller " + controllerClass.getSimpleName() + " deve avere @RequestMapping");
        
        String[] values = requestMapping.value();
        assertTrue(values.length > 0, 
            "Controller " + controllerClass.getSimpleName() + " deve avere un value in @RequestMapping");
        
        return values[0];
    }
    
    /**
     * Trova tutti gli endpoint di refresh (che contengono "refresh" nel path) per un controller
     */
    private Set<String> getRefreshEndpoints(Class<?> controllerClass, String basePath) {
        return Arrays.stream(controllerClass.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PostMapping.class))
            .map(method -> {
                PostMapping postMapping = method.getAnnotation(PostMapping.class);
                String[] values = postMapping.value();
                if (values.length > 0 && values[0].contains("refresh")) {
                    return basePath + values[0];
                }
                return null;
            })
            .filter(path -> path != null)
            .collect(Collectors.toSet());
    }
}
