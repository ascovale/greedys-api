package com.application.common.spring.swagger.catalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.application.common.spring.swagger.metadata.WrapperCategory;
import com.application.common.spring.swagger.util.StringCaseUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Lightweight registry/utility that builds a JSON catalog of ResponseWrapperX -> T mappings
 * and writes it atomically to disk. Intended to be used at startup while OpenAPI schemas are
 * being generated.
 */
public final class ResponseWrapperCatalogRegistry {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules()
            .configure(SerializationFeature.INDENT_OUTPUT, true);

    private static final Set<String> PRIMITIVES = new HashSet<>(List.of(
        "String", "Integer", "Long", "Boolean", "Double", "Float", "Object", "Void",
        "LocalDate", "LocalDateTime"
    ));

    private ResponseWrapperCatalogRegistry() {}

    public static void generateAndSave(Map<String, String> wrapperToClassMapping,
                                       Map<String, WrapperCategory> wrapperToCategory,
                                       Iterable<String> wrapperSchemasToGenerate) {
        try {
            List<CatalogEntry> entries = new ArrayList<>();
            for (String wrapperName : wrapperSchemasToGenerate) {
                String fullClass = wrapperToClassMapping.get(wrapperName);
                String simpleName = simpleFromFull(fullClass);
                WrapperCategory category = wrapperToCategory != null ? wrapperToCategory.get(wrapperName) : null;
                if (category == null) category = deduceCategoryFromName(wrapperName);

                CatalogEntry e = new CatalogEntry();
                e.wrapper = wrapperName;
                e.schemaRef = "#/components/schemas/" + wrapperName;
                e.dataType = simpleName;
                e.dataTypeFull = fullClass;
                e.category = category != null ? category.name() : "SINGLE";
                e.dataTypeSnake = StringCaseUtils.toSnakeCase(simpleName);
                e.isPrimitive = isPrimitive(simpleName);
                e.dartType = mapDartType(simpleName);
                entries.add(e);
            }

            CatalogFile catalog = new CatalogFile();
            catalog.generatedAt = Instant.now().toString();
            catalog.entries = entries;

            Path out = defaultOutputPath();
            writeAtomic(catalog, out);
        } catch (Exception ex) {
            // Avoid throwing from swagger customization - log via stderr as last resort
            System.err.println("Error writing ResponseWrapper catalog: " + ex.getMessage());
        }
    }

    private static boolean isPrimitive(String simpleName) {
        if (simpleName == null) return true;
        return PRIMITIVES.contains(simpleName);
    }

    private static String mapDartType(String simpleName) {
        if (simpleName == null) return null;
        return switch (simpleName) {
            case "String" -> "String";
            case "Integer", "Long" -> "int";
            case "Boolean" -> "bool";
            case "Double", "Float" -> "double";
            case "LocalDate", "LocalDateTime" -> "DateTime";
            case "Void" -> "void";
            default -> null;
        };
    }

    private static String simpleFromFull(String full) {
        if (full == null) return "String";
        int last = full.lastIndexOf('.');
        return last >= 0 ? full.substring(last + 1) : full;
    }

    private static WrapperCategory deduceCategoryFromName(String wrapperSchemaName) {
        if (wrapperSchemaName == null) return WrapperCategory.SINGLE;
        if (wrapperSchemaName.contains("List")) return WrapperCategory.LIST;
        if (wrapperSchemaName.contains("Page")) return WrapperCategory.PAGE;
        if (wrapperSchemaName.contains("Void")) return WrapperCategory.VOID;
        return WrapperCategory.SINGLE;
    }

    private static Path defaultOutputPath() {
        String userDir = System.getProperty("user.dir");
        Path target = Path.of(userDir, "target", "generated-resources");
        try {
            Files.createDirectories(target);
        } catch (IOException e) {
            // ignore
        }
        return target.resolve("response-wrappers.json");
    }

    private static void writeAtomic(Object obj, Path out) throws IOException {
        Path tmp = out.resolveSibling(out.getFileName().toString() + ".tmp");
        byte[] bytes = MAPPER.writeValueAsBytes(obj);
        Files.write(tmp, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    // Simple POJOs for JSON serialization
    public static class CatalogFile {
        public String generatedAt;
        public List<CatalogEntry> entries;
    }

    public static class CatalogEntry {
        public String wrapper;
        public String schemaRef;
        public String dataType; // simple name
        public String dataTypeFull; // full class name if available
        public String category;
        public String dataTypeSnake;
        public boolean isPrimitive;
        public String dartType;
    }
}
