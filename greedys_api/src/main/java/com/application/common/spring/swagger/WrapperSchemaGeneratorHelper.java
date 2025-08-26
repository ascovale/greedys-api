package com.application.common.spring.swagger;

import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class per la generazione di schemi wrapper (ResponseWrapper<T>, List<T>, Page<T>)
 * Questa classe si occupa specificatamente di creare gli schemi per i tipi wrapper
 * che avvolgono i dati reali e utilizzano riferimenti ($ref) invece di definizioni inline.
 */
@Slf4j
public class WrapperSchemaGeneratorHelper {

    /**
     * Genera schemi wrapper per tutti i tipi wrapper raccolti
     * 
     * @param wrapperTypes Set di informazioni sui tipi wrapper
     * @param openApi Specifica OpenAPI per aggiungere schemi
     * @param registry Registry per tracciare tipi processati
     */
    public void generateWrapperSchemas(Set<WrapperTypeInfo> wrapperTypes, OpenAPI openApi, WrapperTypeRegistry registry) {
        @SuppressWarnings("rawtypes")
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        
        for (WrapperTypeInfo wrapperInfo : wrapperTypes) {
            generateSingleWrapperSchema(wrapperInfo, schemas, registry);
        }
    }

    /**
     * Genera uno schema wrapper specifico (ResponseWrapper<T>, List<T>, o Page<T>)
     */
    private void generateSingleWrapperSchema(WrapperTypeInfo wrapperInfo, @SuppressWarnings("rawtypes") Map<String, Schema> schemas, WrapperTypeRegistry registry) {
        String wrapperSchemaName = wrapperInfo.getWrapperSchemaName();
        
        log.warn("⚠️ GENERATOR: Tentativo generazione {}", wrapperSchemaName);
        
        // Controlla se lo schema è già stato generato
        if (registry.getWrapperSchemaName(wrapperInfo.dataClassName, wrapperInfo.wrapperType) != null) {
            log.warn("⚠️ GENERATOR: SALTATO {} - già esistente in registry", wrapperSchemaName);
            return;
        }
        
        if (schemas.containsKey(wrapperSchemaName)) {
            log.warn("⚠️ GENERATOR: SALTATO {} - già esistente in schemas", wrapperSchemaName);
            return;
        }

        log.warn("⚠️ GENERATOR: CREANDO {} per tipo {}", wrapperSchemaName, wrapperInfo.wrapperType);
        
        switch (wrapperInfo.wrapperType) {
            case "DTO":
                generateResponseWrapperSchema(wrapperInfo, schemas, registry);
                break;
            case "LIST":
                generateListWrapperSchema(wrapperInfo, schemas, registry);
                break;
            case "PAGE":
                generatePageWrapperSchema(wrapperInfo, schemas, registry);
                break;
            default:
                log.warn("Tipo wrapper non supportato: {}", wrapperInfo.wrapperType);
        }
    }

    /**
     * Genera schema per ResponseWrapper<T>
     * Struttura: { success: boolean, message: string, data: $ref<T> }
     */
    private void generateResponseWrapperSchema(WrapperTypeInfo wrapperInfo, @SuppressWarnings("rawtypes") Map<String, Schema> schemas, WrapperTypeRegistry registry) {
        String wrapperSchemaName = wrapperInfo.getWrapperSchemaName();
        String dataTypeName = wrapperInfo.getDataTypeSimpleName();

        @SuppressWarnings("rawtypes")
        Schema wrapperSchema = new Schema();
        wrapperSchema.setType("object");
        wrapperSchema.setTitle("Response wrapper for " + dataTypeName);
        
        // Aggiungi x-class-name per nomi di classe più puliti
        String cleanClassName = "ResponseWrapper" + dataTypeName;
        wrapperSchema.addExtension("x-class-name", cleanClassName);
        
        // Aggiungi proprietà del wrapper
        @SuppressWarnings("rawtypes")
        Schema successProperty = new Schema();
        successProperty.setType("boolean");
        successProperty.setDescription("Indicates if the operation was successful");
        
        @SuppressWarnings("rawtypes")
        Schema messageProperty = new Schema();
        messageProperty.setType("string");
        messageProperty.setDescription("Response message");
        
        // Proprietà data che referenzia lo schema del tipo T
        @SuppressWarnings("rawtypes")
        Schema dataProperty = new Schema();
        dataProperty.set$ref("#/components/schemas/" + dataTypeName);
        
        wrapperSchema.addProperty("success", successProperty);
        wrapperSchema.addProperty("message", messageProperty);
        wrapperSchema.addProperty("data", dataProperty);
        
        schemas.put(wrapperSchemaName, wrapperSchema);
        registry.registerWrapperSchema(wrapperInfo.dataClassName, wrapperInfo.wrapperType, wrapperSchemaName);
        
        log.warn("⚠️ GENERATOR: CREATO EFFETTIVAMENTE {} -> $ref {}", wrapperSchemaName, dataTypeName);
    }

    /**
     * Genera schema per List<T>
     * Struttura: { type: array, items: $ref<T> }
     */
    @SuppressWarnings("unchecked")
    private void generateListWrapperSchema(WrapperTypeInfo wrapperInfo, @SuppressWarnings("rawtypes") Map<String, Schema> schemas, WrapperTypeRegistry registry) {
        String wrapperSchemaName = wrapperInfo.getListSchemaName(); // Usa il metodo specifico per List
        String dataTypeName = wrapperInfo.getDataTypeSimpleName();

        @SuppressWarnings("rawtypes")
        Schema listSchema = new Schema();
        listSchema.setType("array");
        listSchema.setTitle("List of " + dataTypeName);
        
        // Aggiungi x-class-name per nomi di classe più puliti
        String cleanClassName = "ResponseWrapperList" + dataTypeName;
        listSchema.addExtension("x-class-name", cleanClassName);
        
        // Items che referenzia lo schema del tipo T
        @SuppressWarnings("rawtypes")
        Schema itemsSchema = new Schema();
        itemsSchema.set$ref("#/components/schemas/" + dataTypeName);
        listSchema.setItems(itemsSchema);
        
        schemas.put(wrapperSchemaName, listSchema);
        registry.registerWrapperSchema(wrapperInfo.dataClassName, "LIST", wrapperSchemaName);
        
        log.warn("⚠️ GENERATOR: CREATO LIST {} -> array of $ref {}", wrapperSchemaName, dataTypeName);
    }

    /**
     * Genera schema per Page<T> (Spring Data)
     * Struttura: { content: array of $ref<T>, pageable: {...}, ... }
     */
    @SuppressWarnings("unchecked")
    private void generatePageWrapperSchema(WrapperTypeInfo wrapperInfo, @SuppressWarnings("rawtypes") Map<String, Schema> schemas, WrapperTypeRegistry registry) {
        String wrapperSchemaName = wrapperInfo.getPageSchemaName(); // Usa il metodo specifico per Page
        String dataTypeName = wrapperInfo.getDataTypeSimpleName();

        @SuppressWarnings("rawtypes")
        Schema pageSchema = new Schema();
        pageSchema.setType("object");
        pageSchema.setTitle("Page of " + dataTypeName);
        
        // Aggiungi x-class-name per nomi di classe più puliti
        String cleanClassName = "ResponseWrapperPage" + dataTypeName;
        pageSchema.addExtension("x-class-name", cleanClassName);
        
        // Proprietà content come array di riferimenti al tipo T
        @SuppressWarnings("rawtypes")
        Schema contentProperty = new Schema();
        contentProperty.setType("array");
        @SuppressWarnings("rawtypes")
        Schema contentItems = new Schema();
        contentItems.set$ref("#/components/schemas/" + dataTypeName);
        contentProperty.setItems(contentItems);
        
        // Proprietà pageable (informazioni di paginazione)
        @SuppressWarnings("rawtypes")
        Schema pageableProperty = new Schema();
        pageableProperty.setType("object");
        pageableProperty.setDescription("Pagination information");
        
        // Proprietà totalElements
        @SuppressWarnings("rawtypes")
        Schema totalElementsProperty = new Schema();
        totalElementsProperty.setType("integer");
        totalElementsProperty.setFormat("int64");
        
        // Proprietà totalPages
        @SuppressWarnings("rawtypes")
        Schema totalPagesProperty = new Schema();
        totalPagesProperty.setType("integer");
        totalPagesProperty.setFormat("int32");
        
        // Proprietà size
        @SuppressWarnings("rawtypes")
        Schema sizeProperty = new Schema();
        sizeProperty.setType("integer");
        sizeProperty.setFormat("int32");
        
        // Proprietà number (current page)
        @SuppressWarnings("rawtypes")
        Schema numberProperty = new Schema();
        numberProperty.setType("integer");
        numberProperty.setFormat("int32");
        
        // Proprietà numberOfElements
        @SuppressWarnings("rawtypes")
        Schema numberOfElementsProperty = new Schema();
        numberOfElementsProperty.setType("integer");
        numberOfElementsProperty.setFormat("int32");
        
        // Proprietà first
        @SuppressWarnings("rawtypes")
        Schema firstProperty = new Schema();
        firstProperty.setType("boolean");
        
        // Proprietà last
        @SuppressWarnings("rawtypes")
        Schema lastProperty = new Schema();
        lastProperty.setType("boolean");
        
        // Proprietà empty
        @SuppressWarnings("rawtypes")
        Schema emptyProperty = new Schema();
        emptyProperty.setType("boolean");
        
        pageSchema.addProperty("content", contentProperty);
        pageSchema.addProperty("pageable", pageableProperty);
        pageSchema.addProperty("totalElements", totalElementsProperty);
        pageSchema.addProperty("totalPages", totalPagesProperty);
        pageSchema.addProperty("size", sizeProperty);
        pageSchema.addProperty("number", numberProperty);
        pageSchema.addProperty("numberOfElements", numberOfElementsProperty);
        pageSchema.addProperty("first", firstProperty);
        pageSchema.addProperty("last", lastProperty);
        pageSchema.addProperty("empty", emptyProperty);
        
        schemas.put(wrapperSchemaName, pageSchema);
        registry.registerWrapperSchema(wrapperInfo.dataClassName, "PAGE", wrapperSchemaName);
        
        log.debug("Schema Page generato: {} -> page of $ref {}", wrapperSchemaName, dataTypeName);
    }
}
