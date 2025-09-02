package com.application.common.spring.swagger.vendor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Registry centralizzato per vendor extensions.
 * 
 * Organizza le extensions per livello e target, permettendo una gestione 
 * efficiente durante la generazione dell'OpenAPI spec.
 */
@Component
@Slf4j
public class VendorExtensionRegistry {
    
    // Organizzazione: Level -> Target -> List<VendorExtension>
    private final Map<VendorExtensionLevel, Map<String, List<VendorExtension>>> extensionsByLevel = new ConcurrentHashMap<>();
    
    /**
     * Registra una vendor extension
     */
    public void register(VendorExtension extension) {
        if (extension == null) {
            log.warn("Attempted to register null vendor extension");
            return;
        }
        
        if (!extension.getName().startsWith("x-")) {
            log.error("Invalid vendor extension name '{}' - must start with 'x-'", extension.getName());
            return;
        }
        
        VendorExtensionLevel level = extension.getLevel();
        String target = extension.getTarget() != null ? extension.getTarget() : "*"; // "*" = all targets
        
        extensionsByLevel
            .computeIfAbsent(level, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(target, k -> new ArrayList<>())
            .add(extension);
            
        // Ordina per priorità (maggiore prima)
        extensionsByLevel.get(level).get(target).sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        
        log.debug("Registered vendor extension '{}' for level {} target {}", extension.getName(), level, target);
    }
    
    /**
     * Ottiene tutte le extensions per un livello specifico
     */
    public List<VendorExtension> getExtensions(VendorExtensionLevel level) {
        return extensionsByLevel.getOrDefault(level, Collections.emptyMap())
                .values()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
    
    /**
     * Ottiene extensions per un livello e target specifici
     */
    public List<VendorExtension> getExtensions(VendorExtensionLevel level, String target) {
        Map<String, List<VendorExtension>> levelExtensions = extensionsByLevel.get(level);
        if (levelExtensions == null) {
            return Collections.emptyList();
        }
        
        List<VendorExtension> result = new ArrayList<>();
        
        // Aggiungi extensions specifiche per il target
        List<VendorExtension> targetSpecific = levelExtensions.get(target);
        if (targetSpecific != null) {
            result.addAll(targetSpecific);
        }
        
        // Aggiungi extensions globali per il livello (target = "*")
        List<VendorExtension> global = levelExtensions.get("*");
        if (global != null) {
            result.addAll(global);
        }
        
        return result;
    }
    
    /**
     * Ottiene extensions applicabili con filtro condizionale
     */
    public List<VendorExtension> getApplicableExtensions(VendorExtensionLevel level, String target, Map<String, Object> context) {
        return getExtensions(level, target)
                .stream()
                .filter(ext -> ext.shouldApply(target, context))
                .collect(Collectors.toList());
    }
    
    /**
     * Statistiche del registry
     */
    public String getStats() {
        int totalExtensions = extensionsByLevel.values()
                .stream()
                .mapToInt(levelMap -> levelMap.values().stream().mapToInt(List::size).sum())
                .sum();
                
        return String.format("VendorExtensionRegistry: %d extensions across %d levels", 
                            totalExtensions, extensionsByLevel.size());
    }
    
    /**
     * Cancella tutte le extensions (per testing)
     */
    public void clear() {
        extensionsByLevel.clear();
        log.debug("Cleared all vendor extensions");
    }
}
