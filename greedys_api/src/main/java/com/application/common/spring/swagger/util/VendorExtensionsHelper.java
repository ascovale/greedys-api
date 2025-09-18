package com.application.common.spring.swagger.util;

import java.util.Map;
import java.util.Set;

import com.application.common.spring.swagger.metadata.OperationDataMetadata;
import com.application.common.spring.swagger.metadata.WrapperCategory;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Helper centralizzato per aggiungere vendor extensions coerenti a operation e schema.
 */
public final class VendorExtensionsHelper {

    private static final Set<String> PRIMITIVES = Set.of(
    "String", "Integer", "Long", "Boolean", "Double", "Float", "Object", "Void",
    "LocalDate", "LocalDateTime"
    );

    private static final Map<String, String> DART_TYPE_MAP = Map.of(
        "String", "String",
        "Integer", "int",
        "Long", "int",
        "Boolean", "bool",
        "Double", "double",
    "Float", "double",
        "Object", "dynamic",
    "Void", "void",
    "LocalDate", "DateTime",
    "LocalDateTime", "DateTime"
    );

    private VendorExtensionsHelper() {}

    public static void addOperationVendorExtensions(Operation operation, OperationDataMetadata metadata) {
        String fullClass = metadata.getDataClassName();
        String simpleName = fullClass != null && !fullClass.trim().isEmpty() ?
            extractSimpleClassName(fullClass) : "String";

        String snake = StringCaseUtils.toSnakeCase(simpleName);

        operation.addExtension("x-generic-type", simpleName);
        operation.addExtension("x-generic-type-snake", snake);

        boolean isPrimitive = isPrimitive(simpleName);
        operation.addExtension("x-generic-is-primitive", isPrimitive);

        if (isPrimitive) {
            String dart = DART_TYPE_MAP.getOrDefault(simpleName, "dynamic");
            operation.addExtension("x-generic-dart-type", dart);
        }

        WrapperCategory category = metadata.getWrapperCategory();
        String categoryName = category != null ? category.name() : WrapperCategory.SINGLE.name();
        operation.addExtension("x-wrapper-type", categoryName);
        operation.addExtension("x-wrapper-type-snake", StringCaseUtils.toSnakeCase(categoryName.toLowerCase()));
    // Boolean flags useful for templates
    boolean isSingle = category == WrapperCategory.SINGLE;
    boolean isList = category == WrapperCategory.LIST;
    boolean isPage = category == WrapperCategory.PAGE;
    operation.addExtension("x-wrapper-type-is-single", isSingle);
    operation.addExtension("x-wrapper-type-is-list", isList);
    operation.addExtension("x-wrapper-type-is-page", isPage);
    }

    public static void addSchemaVendorExtensions(Schema<?> schema, String dataClassName, WrapperCategory category) {
        String simpleName = dataClassName != null && !dataClassName.trim().isEmpty() ?
            extractSimpleClassName(dataClassName) : "String";

        schema.addExtension("x-generic-type", simpleName);
        schema.addExtension("x-generic-type-snake", StringCaseUtils.toSnakeCase(simpleName));

        boolean isPrimitive = isPrimitive(simpleName);
        schema.addExtension("x-generic-is-primitive", isPrimitive);
        if (isPrimitive) {
            schema.addExtension("x-generic-dart-type", DART_TYPE_MAP.getOrDefault(simpleName, "dynamic"));
        }

        String categoryName = category != null ? category.name() : WrapperCategory.SINGLE.name();
        schema.addExtension("x-wrapper-type", categoryName);
        schema.addExtension("x-wrapper-type-snake", StringCaseUtils.toSnakeCase(categoryName.toLowerCase()));
    // Boolean flags useful for templates
    boolean isSingle = category == WrapperCategory.SINGLE;
    boolean isList = category == WrapperCategory.LIST;
    boolean isPage = category == WrapperCategory.PAGE;
    schema.addExtension("x-wrapper-type-is-single", isSingle);
    schema.addExtension("x-wrapper-type-is-list", isList);
    schema.addExtension("x-wrapper-type-is-page", isPage);
    }

    private static boolean isPrimitive(String simpleName) {
        return PRIMITIVES.contains(simpleName);
    }

    private static String extractSimpleClassName(String fullClassName) {
        if (fullClassName == null || fullClassName.trim().isEmpty()) return fullClassName;
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot >= 0 ? fullClassName.substring(lastDot + 1) : fullClassName;
    }
}
