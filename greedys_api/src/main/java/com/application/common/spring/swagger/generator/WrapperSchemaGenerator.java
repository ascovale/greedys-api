package com.application.common.spring.swagger.generator;

import com.application.common.spring.swagger.generator.helper.SchemaHelper;
import com.application.common.spring.swagger.metadata.SchemaMetadata;
import com.application.common.spring.swagger.metadata.WrapperCategory;
import com.application.common.spring.swagger.util.VendorExtensionsHelper;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Generatore per schemi wrapper (ResponseWrapperXXX)
 */
@UtilityClass
@Slf4j
public class WrapperSchemaGenerator {
    
    public static void generateWrapperSchema(SchemaMetadata metadata, OpenAPI openApi) {
        WrapperCategory category = metadata.getWrapperCategory();
        String dataTypeSimpleName = SchemaHelper.extractSimpleClassName(metadata.getDataClassName());
        String schemaName = category.generateSchemaName(dataTypeSimpleName);
        
        Schema<?> wrapperSchema = createBaseWrapperSchema(category);
        addCategorySpecificProperties(wrapperSchema, category, metadata);
        
        openApi.getComponents().getSchemas().put(schemaName, wrapperSchema);
        log.debug("Generated wrapper schema: {}", schemaName);
    }
    
    /**
     * Versione semplificata che accetta solo il nome del schema wrapper
     * Deduce automaticamente la categoria e il tipo di dato dal nome
     */
    public static void generateWrapperSchema(String wrapperSchemaName, OpenAPI openApi) {
        WrapperCategory category = deduceWrapperCategory(wrapperSchemaName);
        String dataType = extractDataTypeFromSchemaName(wrapperSchemaName);
        
        Schema<?> wrapperSchema = createBaseWrapperSchemaSimplified(category, dataType);
        addCategorySpecificPropertiesSimplified(wrapperSchema, category, dataType);
        
        openApi.getComponents().getSchemas().put(wrapperSchemaName, wrapperSchema);
        log.debug("Generated simplified wrapper schema: {}", wrapperSchemaName);
    }
    
    /**
     * Versione che accetta il nome dello schema wrapper e il nome della classe dati.
     * Aggiunge anche i vendor extensions x-generic-type e x-generic-type-snake.
     */
    public static void generateWrapperSchema(String wrapperSchemaName, String dataClassName, OpenAPI openApi) {
        WrapperCategory category = deduceWrapperCategory(wrapperSchemaName);
        String dataType = dataClassName != null ? extractSimpleClassName(dataClassName) : 
                         extractDataTypeFromSchemaName(wrapperSchemaName);
        
        Schema<?> wrapperSchema = createBaseWrapperSchemaSimplified(category, dataType);
        addCategorySpecificPropertiesSimplified(wrapperSchema, category, dataType);
        
        // Aggiungi vendor extensions per lo schema wrapper (tipo, primitive, dart-type, categoria)
        //VendorExtensionsHelper.addSchemaVendorExtensions(wrapperSchema, dataClassName, category);
        
        openApi.getComponents().getSchemas().put(wrapperSchemaName, wrapperSchema);
        log.debug("Generated wrapper schema with extensions: {}", wrapperSchemaName);
    }
    
    /**
     * Versione che accetta nome schema, classe dati e categoria del wrapper.
     * Aggiunge vendor extensions per tipo e categoria.
     */
    public static void generateWrapperSchema(String wrapperSchemaName, String dataClassName, 
                                           WrapperCategory category, OpenAPI openApi) {
        String dataType = dataClassName != null ? extractSimpleClassName(dataClassName) : 
                         extractDataTypeFromSchemaName(wrapperSchemaName);
        
        Schema<?> wrapperSchema = createBaseWrapperSchemaSimplified(category, dataType);
        addCategorySpecificPropertiesSimplified(wrapperSchema, category, dataType);
        
    // Aggiungi vendor extensions per lo schema wrapper (tipo, primitive, dart-type, categoria)
    VendorExtensionsHelper.addSchemaVendorExtensions(wrapperSchema, dataClassName, category);
        
        openApi.getComponents().getSchemas().put(wrapperSchemaName, wrapperSchema);
        log.debug("Generated wrapper schema with type and category extensions: {}", wrapperSchemaName);
    }
    
    private static Schema<?> createBaseWrapperSchema(WrapperCategory category) {
        return new Schema<>()
            .type("object")
            .description("Response wrapper for " + category.name().toLowerCase() + " operations")
            .addProperty("message", new Schema<>().type("string").example("Operation completed successfully"))
            .addProperty("timestamp", new Schema<>().type("string").format("date-time"));
    }
    
    private static void addCategorySpecificProperties(Schema<?> wrapperSchema, WrapperCategory category, SchemaMetadata metadata) {
        switch (category) {
            case SINGLE -> addSingleProperties(wrapperSchema, metadata);
            case LIST -> addListProperties(wrapperSchema, metadata);
            case PAGE -> addPageProperties(wrapperSchema, metadata);
            case SLICE -> addSliceProperties(wrapperSchema, metadata);
            case VOID -> addVoidProperties(wrapperSchema);
        }
    }
    
    private static void addSingleProperties(Schema<?> wrapperSchema, SchemaMetadata metadata) {
        if (metadata.getDataClassName() != null) {
            Schema<?> dataSchema = SchemaHelper.createDataSchema(metadata.getDataClassName());
            wrapperSchema.addProperty("data", dataSchema);
        } else {
            wrapperSchema.addProperty("data", new Schema<>().type("object"));
        }
        wrapperSchema.addProperty("metadata", new Schema<>().$ref("#/components/schemas/SingleMetadata"));
    }
    
    private static void addListProperties(Schema<?> wrapperSchema, SchemaMetadata metadata) {
        Schema<?> listSchema = new Schema<>().type("array");
        if (metadata.getDataClassName() != null) {
            Schema<?> itemSchema = SchemaHelper.createItemSchema(metadata.getDataClassName());
            listSchema.items(itemSchema);
        } else {
            listSchema.items(new Schema<>().type("object"));
        }
        wrapperSchema.addProperty("data", listSchema);
        wrapperSchema.addProperty("metadata", new Schema<>().$ref("#/components/schemas/ListMetadata"));
    }
    
    @SuppressWarnings("unchecked")
    private static void addPageProperties(Schema<?> wrapperSchema, SchemaMetadata metadata) {
        Schema<?> arrayItemsSchema = metadata.getDataClassName() != null ? 
            SchemaHelper.createItemSchema(metadata.getDataClassName()) : 
            new Schema<>().type("object");
            
        Schema<?> pageSchema = new Schema<>().type("object")
            .addProperty("content", new Schema<>().type("array").items((Schema<Object>) arrayItemsSchema))
            .addProperty("totalElements", new Schema<>().type("integer").format("int64"))
            .addProperty("totalPages", new Schema<>().type("integer"))
            .addProperty("size", new Schema<>().type("integer"))
            .addProperty("number", new Schema<>().type("integer"));
            
        wrapperSchema.addProperty("data", pageSchema);
        wrapperSchema.addProperty("metadata", new Schema<>().$ref("#/components/schemas/PageMetadata"));
    }
    
    @SuppressWarnings("unchecked")
    private static void addSliceProperties(Schema<?> wrapperSchema, SchemaMetadata metadata) {
        Schema<?> arrayItemsSchema = metadata.getDataClassName() != null ? 
            SchemaHelper.createItemSchema(metadata.getDataClassName()) : 
            new Schema<>().type("object");
            
        Schema<?> sliceSchema = new Schema<>().type("object")
            .addProperty("content", new Schema<>().type("array").items((Schema<Object>) arrayItemsSchema))
            .addProperty("size", new Schema<>().type("integer"))
            .addProperty("number", new Schema<>().type("integer"))
            .addProperty("numberOfElements", new Schema<>().type("integer"))
            .addProperty("first", new Schema<>().type("boolean"))
            .addProperty("last", new Schema<>().type("boolean"))
            .addProperty("hasNext", new Schema<>().type("boolean"))
            .addProperty("hasPrevious", new Schema<>().type("boolean"));
            
        wrapperSchema.addProperty("data", sliceSchema);
        wrapperSchema.addProperty("metadata", new Schema<>().$ref("#/components/schemas/BaseMetadata"));
    }
    
    private static void addVoidProperties(Schema<?> wrapperSchema) {
        wrapperSchema.addProperty("data", new Schema<>().type("string").example("Operation completed"));
    }
    
    // === METODI HELPER PER VERSIONE SEMPLIFICATA ===
    
    /**
     * Deduce la WrapperCategory dal nome dello schema
     * Es: "ResponseWrapperListReservationDTO" -> LIST
     */
    private static WrapperCategory deduceWrapperCategory(String wrapperSchemaName) {
        if (wrapperSchemaName.contains("List")) return WrapperCategory.LIST;
        if (wrapperSchemaName.contains("Page")) return WrapperCategory.PAGE;
        if (wrapperSchemaName.contains("Slice")) return WrapperCategory.SLICE;
        if (wrapperSchemaName.contains("Void")) return WrapperCategory.VOID;
        return WrapperCategory.SINGLE;
    }
    
    /**
     * Estrae il tipo di dato dal nome dello schema
     * Es: "ResponseWrapperListReservationDTO" -> "ReservationDTO"
     */
    private static String extractDataTypeFromSchemaName(String wrapperSchemaName) {
        // Rimuovi "ResponseWrapper" prefix
        String withoutPrefix = wrapperSchemaName.replace("ResponseWrapper", "");
        
        // Rimuovi categorie
        withoutPrefix = withoutPrefix.replace("List", "").replace("Page", "").replace("Slice", "").replace("Void", "");
        
        // Se rimane vuoto, default a String
        return withoutPrefix.isEmpty() ? "String" : withoutPrefix;
    }
    
    /**
     * Crea schema wrapper base semplificato
     */
    private static Schema<?> createBaseWrapperSchemaSimplified(WrapperCategory category, String dataType) {
        String description = "ResponseWrapperString".equals("ResponseWrapper" + dataType) ? 
            "Response wrapper for single operations" :
            "Response wrapper for " + category.name().toLowerCase() + " operations with " + dataType;
            
        return new Schema<>()
            .type("object")
            .description(description)
            .addProperty("message", new Schema<>().type("string").example("Operation completed successfully"))
            .addProperty("timestamp", new Schema<>().type("string").format("date-time"));
    }
    
    /**
     * Aggiunge proprietà specifiche per categoria (versione semplificata)
     */
    @SuppressWarnings("unchecked")
    private static void addCategorySpecificPropertiesSimplified(Schema<?> wrapperSchema, WrapperCategory category, String dataType) {
        switch (category) {
            case SINGLE -> {
                if ("String".equals(dataType)) {
                    // Per ResponseWrapperString, usa description al posto di $ref per data
                    wrapperSchema.addProperty("data", new Schema<>().description("Response data"));
                } else {
                    // Usa SchemaHelper per gli altri tipi
                    String fullClassName = mapToFullClassName(dataType);
                    Schema<?> dataSchema = SchemaHelper.createDataSchema(fullClassName);
                    wrapperSchema.addProperty("data", dataSchema);
                }
                // Aggiungi metadata per tutti i tipi SINGLE
                wrapperSchema.addProperty("metadata", new Schema<>().$ref("#/components/schemas/BaseMetadata"));
            }
            case LIST -> {
                String fullClassName = mapToFullClassName(dataType);
                Schema<?> itemSchema = SchemaHelper.createItemSchema(fullClassName);
                wrapperSchema.addProperty("data", new Schema<>().type("array").items((Schema<Object>) itemSchema));
                wrapperSchema.addProperty("metadata", new Schema<>().$ref("#/components/schemas/BaseMetadata"));
            }
            case PAGE -> {
                String fullClassName = mapToFullClassName(dataType);
                Schema<?> itemSchema = SchemaHelper.createItemSchema(fullClassName);
                Schema<?> pageSchema = new Schema<>().type("object")
                    .addProperty("content", new Schema<>().type("array").items((Schema<Object>) itemSchema))
                    .addProperty("totalElements", new Schema<>().type("integer").format("int64"))
                    .addProperty("totalPages", new Schema<>().type("integer"))
                    .addProperty("size", new Schema<>().type("integer"))
                    .addProperty("number", new Schema<>().type("integer"));
                wrapperSchema.addProperty("data", pageSchema);
                wrapperSchema.addProperty("metadata", new Schema<>().$ref("#/components/schemas/BaseMetadata"));
            }
            case SLICE -> {
                String fullClassName = mapToFullClassName(dataType);
                Schema<?> itemSchema = SchemaHelper.createItemSchema(fullClassName);
                Schema<?> sliceSchema = new Schema<>().type("object")
                    .addProperty("content", new Schema<>().type("array").items((Schema<Object>) itemSchema))
                    .addProperty("size", new Schema<>().type("integer"))
                    .addProperty("number", new Schema<>().type("integer"))
                    .addProperty("numberOfElements", new Schema<>().type("integer"))
                    .addProperty("first", new Schema<>().type("boolean"))
                    .addProperty("last", new Schema<>().type("boolean"))
                    .addProperty("hasNext", new Schema<>().type("boolean"))
                    .addProperty("hasPrevious", new Schema<>().type("boolean"));
                wrapperSchema.addProperty("data", sliceSchema);
                wrapperSchema.addProperty("metadata", new Schema<>().$ref("#/components/schemas/BaseMetadata"));
            }
            case VOID -> {
                wrapperSchema.addProperty("data", new Schema<>().type("string").example("Operation completed"));
            }
        }
    }
    
    /**
     * Mappa i nomi dei tipi semplici ai nomi delle classi complete
     */
    private static String mapToFullClassName(String dataType) {
        return switch (dataType) {
            case "String" -> "java.lang.String";
            case "Long" -> "java.lang.Long";
            case "Integer" -> "java.lang.Integer";
            case "Boolean" -> "java.lang.Boolean";
            case "Double" -> "java.lang.Double";
            case "Float" -> "java.lang.Float";
            case "LocalDate" -> "java.time.LocalDate";
            case "LocalDateTime" -> "java.time.LocalDateTime";
            default -> dataType; // Assume sia già un nome completo o un DTO
        };
    }
    
    /**
     * Estrae il nome semplice della classe dal nome completo (rimuove il package).
     */
    private static String extractSimpleClassName(String fullClassName) {
        if (fullClassName == null || fullClassName.trim().isEmpty()) {
            return fullClassName;
        }
        
        return fullClassName.contains(".") 
            ? fullClassName.substring(fullClassName.lastIndexOf('.') + 1)
            : fullClassName;
    }
}
