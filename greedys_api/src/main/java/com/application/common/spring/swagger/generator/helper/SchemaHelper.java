package com.application.common.spring.swagger.generator.helper;

import io.swagger.v3.oas.models.media.Schema;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper per la creazione di schemi per tipi primitivi e DTO
 */
@UtilityClass
@Slf4j
public class SchemaHelper {
    
    /**
     * Crea lo schema per gli items di un array in base al tipo della classe
     * Per tipi primitivi usa la definizione diretta, per DTO usa $ref
     */
    public static Schema<?> createItemSchema(String dataClassName) {
        // Gestione dei tipi primitivi Java e Wrapper
        switch (dataClassName) {
            case "java.lang.String":
                return new Schema<>().type("string");
            
            case "java.lang.Long":
                return new Schema<>()
                    .type("integer")
                    .format("int64");
            
            case "java.lang.Integer":
                return new Schema<>().type("integer");
            
            case "java.lang.Boolean":
                return new Schema<>().type("boolean");
            
            case "java.lang.Double":
                return new Schema<>()
                    .type("number")
                    .format("double");
            
            case "java.lang.Float":
                return new Schema<>()
                    .type("number")
                    .format("float");
            
            case "java.time.LocalDate":
                return new Schema<>()
                    .type("string")
                    .format("date");
            
            case "java.time.LocalDateTime":
                return new Schema<>()
                    .type("string")
                    .format("date-time");
            
            // Tipi primitivi Java
            case "long":
                return new Schema<>()
                    .type("integer")
                    .format("int64");
            
            case "int":
                return new Schema<>().type("integer");
            
            case "boolean":
                return new Schema<>().type("boolean");
            
            case "double":
                return new Schema<>()
                    .type("number")
                    .format("double");
            
            case "float":
                return new Schema<>()
                    .type("number")
                    .format("float");
            
            // Per tutti gli altri tipi (DTO), usa il reference
            default:
                String simpleClassName = extractSimpleClassName(dataClassName);
                return new Schema<>().$ref("#/components/schemas/" + simpleClassName);
        }
    }
    
    /**
     * Crea lo schema corretto per il campo data in base al tipo della classe
     * Per tipi primitivi usa la definizione diretta, per DTO usa $ref
     */
    public static Schema<?> createDataSchema(String dataClassName) {
        // Gestione dei tipi primitivi Java e Wrapper
        switch (dataClassName) {
            case "java.lang.String":
                return new Schema<>()
                    .type("string")
                    .description("Response data");
            
            case "java.lang.Long":
                return new Schema<>()
                    .type("integer")
                    .format("int64")
                    .description("Response data");
            
            case "java.lang.Integer":
                return new Schema<>()
                    .type("integer")
                    .description("Response data");
            
            case "java.lang.Boolean":
                return new Schema<>()
                    .type("boolean")
                    .description("Response data");
            
            case "java.lang.Double":
                return new Schema<>()
                    .type("number")
                    .format("double")
                    .description("Response data");
            
            case "java.lang.Float":
                return new Schema<>()
                    .type("number")
                    .format("float")
                    .description("Response data");
            
            case "java.time.LocalDate":
                return new Schema<>()
                    .type("string")
                    .format("date")
                    .description("Response data");
            
            case "java.time.LocalDateTime":
                return new Schema<>()
                    .type("string")
                    .format("date-time")
                    .description("Response data");
            
            // Tipi primitivi Java
            case "long":
                return new Schema<>()
                    .type("integer")
                    .format("int64")
                    .description("Response data");
            
            case "int":
                return new Schema<>()
                    .type("integer")
                    .description("Response data");
            
            case "boolean":
                return new Schema<>()
                    .type("boolean")
                    .description("Response data");
            
            case "double":
                return new Schema<>()
                    .type("number")
                    .format("double")
                    .description("Response data");
            
            case "float":
                return new Schema<>()
                    .type("number")
                    .format("float")
                    .description("Response data");
            
            // Per tutti gli altri tipi (DTO), usa il reference
            default:
                String simpleClassName = extractSimpleClassName(dataClassName);
                return new Schema<>().$ref("#/components/schemas/" + simpleClassName);
        }
    }
    
    /**
     * Estrae il nome semplice della classe dal nome completo
     */
    public static String extractSimpleClassName(String fullClassName) {
        if (fullClassName == null || fullClassName.trim().isEmpty()) {
            return "UnknownClass";
        }
        return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
    }
}
