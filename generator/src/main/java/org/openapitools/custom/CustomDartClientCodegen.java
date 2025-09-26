package org.openapitools.custom;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.DartDioClientCodegen;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomDartClientCodegen extends DartDioClientCodegen {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomDartClientCodegen.class);
    
    // === COSTANTI ===
    private static final Set<String> PRIMITIVE_DART_TYPES = new LinkedHashSet<>(Arrays.asList(
        "int", "double", "num", "bool", "String", "DateTime", "Object", "dynamic", "void", "Null",
        // Tipi Java primitivi che non devono essere importati come modelli
        "Long", "Integer", "Boolean", "Double", "Float", "Byte", "Short", "Character",
        // Tipi comuni che non sono modelli
        "long", "boolean", "string",
        // Tipi Java di date e tempo
        "LocalDate", "LocalDateTime", "LocalTime", "Date", "Instant", "ZonedDateTime",
        // Altri tipi comuni Java
        "BigDecimal", "BigInteger", "UUID", "URI", "URL"
    ));
    
    private static final String PRIMITIVE_IMPORTS_REGEX = 
        ".*/src/model/(string|boolean|int|long|double|num|list|map)\\.dart$";
    
    private static final String MODEL_PATH_TEMPLATE = "/src/model/";
    private static final String PACKAGE_PREFIX = "package:";
    private static final String API_UTIL_PATH = "/src/api_util.dart";
    
    private Map<String, WrapperInfo> wrapperMap = new HashMap<>();

    /**
     * Estrae il nome del modello dal path dell'import
     * Es: "package:openapi/src/model/response_wrapper_page_customer_dto_data.dart" -> "ResponseWrapperPageCustomerDTOData"
     */
    private String extractModelNameFromImport(String importStr) {
        if (importStr == null || !importStr.contains("/model/")) {
            return null;
        }
        
        try {
            // Estrae la parte dopo "/model/" e rimuove ".dart"
            int modelIndex = importStr.indexOf("/model/");
            if (modelIndex == -1) return null;
            
            String afterModel = importStr.substring(modelIndex + "/model/".length());
            if (afterModel.endsWith(".dart")) {
                String fileName = afterModel.substring(0, afterModel.length() - ".dart".length());
                // Converte da snake_case a PascalCase (es: response_wrapper_page_customer_dto_data -> ResponseWrapperPageCustomerDTOData)
                return snakeCaseToPascalCase(fileName);
            }
        } catch (Exception e) {
            LOGGER.warn("[CustomDartClientCodegen] Could not extract model name from import: " + importStr + " - " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Converte da snake_case a PascalCase
     */
    private String snakeCaseToPascalCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
            return snakeCase;
        }
        
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }

    /**
     * Pulisce la lista import rimuovendo duplicati, primitivi e wrapper non necessari
     */
    private Set<String> cleanImportList(Collection<String> imports, String pubName) {
        if (imports == null || imports.isEmpty()) {
            return new LinkedHashSet<>();
        }
        
        String apiUtilImport = PACKAGE_PREFIX + pubName + API_UTIL_PATH;
        
        Set<String> cleaned = new LinkedHashSet<>();
        for (String importStr : imports) {
            if (importStr == null) continue;
            
            // Skip TUTTI gli import dei wrapper ResponseWrapper* 
            // ECCEZIONE: mantieni sempre ResponseWrapperErrorDetails
            boolean isKnownWrapper = false;
            
            // 1. Controlla wrapper definiti nel file response-wrappers.json
            for (String wrapperName : wrapperMap.keySet()) {
                if (importStr.contains("/" + toModelFilename(wrapperName) + ".dart") && 
                    !wrapperName.equals("ResponseWrapperErrorDetails")) {
                    isKnownWrapper = true;
                    LOGGER.info("[CustomDartClientCodegen] Skipping import for known wrapper: " + wrapperName + " -> " + importStr);
                    break;
                }
            }
            
            // 2. Controlla pattern ResponseWrapper* non catalogati usando stessa logica di postProcessModels
            if (!isKnownWrapper && importStr.contains("/response_wrapper")) {
                // Estrae il nome del modello dal path dell'import
                String modelName = extractModelNameFromImport(importStr);
                if (modelName != null) {
                    // Usa la stessa logica di isResponseWrapper del postProcessModels
                    boolean isResponseWrapper = modelName.startsWith("ResponseWrapper") && 
                        !modelName.equals("ResponseWrapperErrorDetails");
                    
                    if (isResponseWrapper) {
                        isKnownWrapper = true;
                        LOGGER.info("[CustomDartClientCodegen] Skipping import for pattern-based ResponseWrapper: " + modelName + " -> " + importStr);
                    }
                }
            }
            
            if (isKnownWrapper) {
                continue;
            }
            
            // Skip primitive model imports
            if (importStr.matches(PRIMITIVE_IMPORTS_REGEX)) {
                continue;
            }
            
            // Skip api_util import (aggiunto dal template)
            if (apiUtilImport.equals(importStr)) {
                continue;
            }
            
            // Skip bare class names senza package
            if (!importStr.contains(PACKAGE_PREFIX) && !importStr.contains("/") && !importStr.contains(".dart")) {
                continue;
            }
            
            cleaned.add(importStr);
        }
        
        return cleaned;
    }

    /**
     * Aggiunge import per modello se non è primitivo
     */
    private void addModelImportIfNeeded(String modelType, String pubName, Set<String> targetImports) {
        if (modelType == null || modelType.isEmpty()) return;
        if (PRIMITIVE_DART_TYPES.contains(modelType) || "Date".equals(modelType)) return;
        
        String modelFile = toModelFilename(modelType) + ".dart";
        String importPath = PACKAGE_PREFIX + pubName + MODEL_PATH_TEMPLATE + modelFile;
        targetImports.add(importPath);
    }

    @Override
    public void processOpts() {
        super.processOpts();
        //TODO DA TESTARE QUESTA PARTE!!!
        // Force main export file to always be named 'openapi.dart' regardless of pubName
        supportingFiles.removeIf(supportingFile -> 
            supportingFile.getTemplateFile().equals("openapi.mustache"));
        supportingFiles.add(new SupportingFile("openapi.mustache", "lib", "openapi.dart"));

        try {
            // Prima prova nel percorso corrente, poi nei percorsi relativi comuni
            File jsonFile = new File("response-wrappers.json");
            if (!jsonFile.exists()) {
                jsonFile = new File("../response-wrappers.json");
            }
            if (!jsonFile.exists()) {
                jsonFile = new File("../../restaurant_app/response-wrappers.json");
            }
            if (!jsonFile.exists()) {
                LOGGER.info("[CustomDartClientCodegen] No response-wrappers.json found in common paths");
                return;
            }
            LOGGER.info("[CustomDartClientCodegen] Using response-wrappers.json at: " + jsonFile.getAbsolutePath());
            String content = new String(Files.readAllBytes(jsonFile.toPath()), StandardCharsets.UTF_8);
            
            // Remove BOM if present
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }
            
            // Trim whitespace
            content = content.trim();
            
            LOGGER.info("[CustomDartClientCodegen] JSON content length: " + content.length());
            LOGGER.info("[CustomDartClientCodegen] JSON first 50 chars: " + 
                       (content.length() > 50 ? content.substring(0, 50) : content));
            
            JSONObject root = new JSONObject(content);
            JSONObject data = root.optJSONObject("data");
            if (data == null) {
                LOGGER.info("[CustomDartClientCodegen] response-wrappers.json has no 'data' object");
                return;
            }
            JSONArray entries = data.optJSONArray("entries");
            if (entries == null) {
                LOGGER.info("[CustomDartClientCodegen] response-wrappers.json has no 'entries' array in data");
                return;
            }

            for (int i = 0; i < entries.length(); i++) {
                JSONObject obj = entries.getJSONObject(i);
                String wrapperName = obj.getString("wrapper");
                WrapperInfo info = WrapperInfo.fromJson(obj);
                wrapperMap.put(wrapperName, info);
            }
            if (!wrapperMap.isEmpty()) {
                LOGGER.info("[CustomDartClientCodegen] Wrapper JSON loaded with " + wrapperMap.size() + " wrapper types.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Errore nel parsing di response-wrappers.json", e);
        }
        
        LOGGER.info("[CustomDartClientCodegen] Wrapper JSON loaded with " + wrapperMap.size() + " wrapper types.");
        
        // Aggiungi docs/test per il ResponseWrapper<T> da common
        addResponseWrapperDocsAndTests();
    }
    
    /**
     * Aggiunge documentazione e test per il ResponseWrapper<T> generico da common
     */
    private void addResponseWrapperDocsAndTests() {
        // Aggiungi documentazione per ResponseWrapper<T> generico
        supportingFiles.add(new SupportingFile("response_wrapper_doc.mustache", "doc", "ResponseWrapper.md"));
        
        // Aggiungi test per ResponseWrapper<T> generico  
        supportingFiles.add(new SupportingFile("response_wrapper_test.mustache", "test", "response_wrapper_test.dart"));
        
        LOGGER.info("[CustomDartClientCodegen] Added docs and tests for ResponseWrapper<T> from common package");
    }

    /**
     * Override per controllare quali file template devono essere processati
     * Salta la generazione di docs/test solo per i ResponseWrapper models specifici
     * ECCEZIONE: ResponseWrapperErrorDetails (il generico viene dal package common)
     */
    @Override
    public boolean shouldOverwrite(String filename) {
        if (filename != null) {
            // Skip docs/test files per ResponseWrapper models specifici
            // MANTIENI: Solo ResponseWrapperErrorDetails (il generico è in common)
            if (filename.contains("response_wrapper") && 
                !filename.contains("response_wrapper_error_details") &&  // Eccezione: ErrorDetails
                (filename.endsWith("_doc.md") || filename.endsWith("_test.dart"))) {
                
                LOGGER.info("[CustomDartClientCodegen] Skipping doc/test generation for specific ResponseWrapper file: " + filename);
                return false; // Non sovrascrivere = non generare
            }
        }
        return super.shouldOverwrite(filename);
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap operations, List<ModelMap> models) {
        OperationsMap processed = super.postProcessOperationsWithModels(operations, models);
        if (processed == null) return operations;

        OperationMap opMap = processed.getOperations();
        if (opMap == null) return processed;

        List<CodegenOperation> opList = opMap.getOperation();
        if (opList == null) return processed;

        String pubName = resolvePubName();
        LinkedHashSet<String> requiredInnerImports = new LinkedHashSet<>();

        // Processa ogni operazione individualmente
        processOperations(opList, pubName, requiredInnerImports);

        // Gestisce gli import aggregati a livello operazioni
        processAggregatedImports(processed, opMap, opList, pubName, requiredInnerImports);

        return processed;
    }

    /**
     * Risolve il nome del package pub (fallback a 'openapi')
     */
    private String resolvePubName() {
        String pubName = null;
        try {
            Object pn = this.additionalProperties().get("pubName");
            if (pn instanceof String) pubName = (String) pn;
        } catch (Exception ignore) {}
        if (pubName == null || pubName.isEmpty()) pubName = "openapi";
        return pubName;
    }

    /**
     * Processa tutte le operazioni per ResponseWrapper e import
     */
    private void processOperations(List<CodegenOperation> opList, String pubName, LinkedHashSet<String> requiredInnerImports) {
        for (CodegenOperation op : opList) {
            if (op == null) continue;

            processResponseWrapper(op, pubName, requiredInnerImports);
            processGenericTypeImports(op, pubName, requiredInnerImports);
            cleanOperationImports(op, pubName);
            processBodyParameterImports(op, pubName, requiredInnerImports);
        }
    }

    /**
     * Gestisce ResponseWrapper per un'operazione
     */
    private void processResponseWrapper(CodegenOperation op, String pubName, LinkedHashSet<String> requiredInnerImports) {
        if (op.returnType != null && wrapperMap.containsKey(op.returnType)) {
            WrapperInfo info = wrapperMap.get(op.returnType);
            Map<String, Object> ve = op.vendorExtensions;
            if (ve != null) {
                ve.put("x-isResponseWrapper", true);
                ve.put("x-wrappedType", info.getWrappedType());
                ve.put("x-wrapperMode", info.getMode());
            }

            // Ensure inner wrapped model import is present when non-primitive
            String inner = info.getWrappedType();
            if (inner != null && !PRIMITIVE_DART_TYPES.contains(inner) && !info.isPrimitive()) {
                addModelImportIfNeeded(inner, pubName, requiredInnerImports);
                if (op.imports == null) {
                    op.imports = new LinkedHashSet<>();
                }
                addModelImportIfNeeded(inner, pubName, op.imports);
            }
        }
    }

    /**
     * Gestisce import per tipi generici nelle vendor extensions
     */
    private void processGenericTypeImports(CodegenOperation op, String pubName, LinkedHashSet<String> requiredInnerImports) {
        try {
            Map<String, Object> ve = op.vendorExtensions;
            if (ve != null) {
                Object gen = ve.get("x-generic-type");
                if (gen != null) {
                    String inner = String.valueOf(gen);
                    if (inner != null && !inner.isEmpty() && !PRIMITIVE_DART_TYPES.contains(inner) && !"Date".equals(inner)) {
                        addModelImportIfNeeded(inner, pubName, requiredInnerImports);
                        if (op.imports == null) op.imports = new LinkedHashSet<>();
                        addModelImportIfNeeded(inner, pubName, op.imports);
                    }
                }
            }
        } catch (Exception ignore) {}
    }

    /**
     * Pulisce gli import di un'operazione
     */
    private void cleanOperationImports(CodegenOperation op, String pubName) {
        if (op.imports != null) {
            Set<String> cleaned = cleanImportList(op.imports, pubName);
            op.imports.clear();
            op.imports.addAll(cleaned);
        }
    }

    /**
     * Gestisce import per parametri body
     */
    private void processBodyParameterImports(CodegenOperation op, String pubName, LinkedHashSet<String> requiredInnerImports) {
        try {
            if (op.allParams != null) {
                for (CodegenParameter p : op.allParams) {
                    if (p == null) continue;
                    if (!Boolean.TRUE.equals(p.isBodyParam)) continue;
                    addModelImportIfNeeded(p.dataType, pubName, requiredInnerImports);
                    if (op.imports == null) op.imports = new LinkedHashSet<>();
                    addModelImportIfNeeded(p.dataType, pubName, op.imports);
                }
            }
        } catch (Exception ignore) {}
    }

    /**
     * Gestisce gli import aggregati a livello operazioni
     */
    private void processAggregatedImports(OperationsMap processed, OperationMap opMap, List<CodegenOperation> opList, 
                                        String pubName, LinkedHashSet<String> requiredInnerImports) {
        try {
            LinkedHashSet<String> agg = collectAggregatedImports(processed, pubName);
            mergeOperationImports(agg, opList);
            agg.addAll(requiredInnerImports);
            addDateImportIfNeeded(agg, opList, pubName);

            ArrayList<String> cleaned = new ArrayList<>(agg);
            boolean hasClean = !cleaned.isEmpty();
            
            logImportsDebugInfo(opMap, cleaned, hasClean, processed);
            setImportsOnMaps(opMap, processed, cleaned, hasClean);
        } catch (Exception e) {
            LOGGER.warn("[CustomDartClientCodegen] Could not clean operations-level imports: " + e.getMessage());
        }
    }

    /**
     * Raccoglie import aggregati dal processed object
     */
    private LinkedHashSet<String> collectAggregatedImports(OperationsMap processed, String pubName) {
        Object impObj = processed.get("imports");
        LinkedHashSet<String> agg = new LinkedHashSet<>();

        if (impObj instanceof List) {
            for (Object o : (List<?>) impObj) {
                if (o == null) continue;
                String impStr = extractImportString(o);
                if (impStr != null) {
                    agg.add(impStr);
                }
            }
            Set<String> cleanedAgg = cleanImportList(agg, pubName);
            agg.clear();
            agg.addAll(cleanedAgg);
        }
        return agg;
    }

    /**
     * Estrae stringa import da vari tipi di oggetti
     */
    private String extractImportString(Object o) {
        if (o instanceof String) {
            return (String) o;
        } else if (o instanceof Map) {
            Object v = ((Map<?, ?>) o).get("import");
            if (v != null) return String.valueOf(v);
        } else {
            return String.valueOf(o);
        }
        return null;
    }

    /**
     * Merge import dalle operazioni negli import aggregati
     */
    private void mergeOperationImports(LinkedHashSet<String> agg, List<CodegenOperation> opList) {
        if (opList != null) {
            for (CodegenOperation op2 : opList) {
                if (op2 != null && op2.imports != null) {
                    agg.addAll(op2.imports);
                }
            }
        }
    }

    /**
     * Aggiunge import date.dart se necessario
     */
    private void addDateImportIfNeeded(LinkedHashSet<String> agg, List<CodegenOperation> opList, String pubName) {
        try {
            boolean needsDate = false;
            if (opList != null) {
                for (CodegenOperation op2 : opList) {
                    if (op2 == null || op2.allParams == null) continue;
                    for (CodegenParameter p : op2.allParams) {
                        if (p != null && "Date".equals(p.dataType)) {
                            needsDate = true;
                            break;
                        }
                    }
                    if (needsDate) break;
                }
            }
            if (needsDate) {
                agg.add(PACKAGE_PREFIX + pubName + MODEL_PATH_TEMPLATE + "date.dart");
            }
        } catch (Exception ignore) {}
    }

    /**
     * Log informazioni debug sugli import
     */
    private void logImportsDebugInfo(OperationMap opMap, ArrayList<String> cleaned, boolean hasClean, OperationsMap processed) {
        if (hasClean && opMap != null) {
            Object classname = opMap.get("classname");
            LOGGER.info("[CustomDartClientCodegen] For API " + classname + ", importsClean contains:");
            for (String imp : cleaned) {
                LOGGER.info("  - '" + imp + "'");
            }
            
            Object origImports = processed.get("imports");
            if (origImports != null) {
                LOGGER.info("[CustomDartClientCodegen] Original imports for " + classname + ":");
                if (origImports instanceof List) {
                    for (Object o : (List<?>) origImports) {
                        String impStr = extractImportString(o);
                        if (impStr != null) {
                            LOGGER.info("  - '" + impStr + "'");
                        }
                    }
                }
            }
        }
    }

    /**
     * Imposta gli import sulle mappe di output
     */
    private void setImportsOnMaps(OperationMap opMap, OperationsMap processed, ArrayList<String> cleaned, boolean hasClean) {
        opMap.put("hasImportsClean", hasClean);
        processed.put("hasImportsClean", hasClean);
        if (hasClean) {
            opMap.put("importsClean", cleaned);
            processed.put("importsClean", cleaned);
        } else {
            opMap.remove("importsClean");
            processed.remove("importsClean");
        }
    }

    @Override
    public ModelsMap postProcessModels(ModelsMap objs) {
        ModelsMap modelsMap = super.postProcessModels(objs);
        try {
            List<ModelMap> modelMaps = modelsMap.getModels();
            if (modelMaps == null) return modelsMap;
            List<ModelMap> filtered = new ArrayList<>();
            for (ModelMap mm : modelMaps) {
                if (mm == null) continue;
                CodegenModel model = mm.getModel();
                
                // DEBUG: Log tutti i modelli che finiscono con "Data"
                if (model != null && model.classname != null && model.classname.endsWith("Data")) {
                    LOGGER.info("[CustomDartClientCodegen] DEBUG: Found model ending with 'Data': " + model.classname);
                }
                
                // Elimina TUTTI i wrapper ResponseWrapper* ECCETTO ResponseWrapperErrorDetails
                // 1. Wrapper definiti nel file response-wrappers.json del server
                // 2. Wrapper con pattern ResponseWrapper* non catalogati (modelli malformati)
                boolean isResponseWrapper = model != null && model.classname != null && 
                    model.classname.startsWith("ResponseWrapper") && 
                    !model.classname.equals("ResponseWrapperErrorDetails");
                
                if (isResponseWrapper) {
                    String source = (model.classname != null && wrapperMap.containsKey(model.classname)) ? 
                        "defined in response-wrappers.json" : "pattern-based (uncatalogued)";
                    LOGGER.info("[CustomDartClientCodegen] Removing ResponseWrapper model: " + model.classname + 
                               " (" + source + ")");
                    if (model.vendorExtensions != null) {
                        model.vendorExtensions.put("x-isResponseWrapper", true);
                    }
                    if (model.imports != null) {
                        model.imports.clear();
                    }
                    // skip adding wrappers to the list so they are not exported or have docs/tests
                } else {
                    if (model != null && model.classname.equals("ResponseWrapperErrorDetails")) {
                        LOGGER.info("[CustomDartClientCodegen] Keeping ResponseWrapperErrorDetails model for error handling");
                    }
                    // DEBUG: Log quando un modello ResponseWrapper*Data viene mantenuto
                    if (model != null && model.classname != null && 
                        model.classname.startsWith("ResponseWrapper") && model.classname.endsWith("Data")) {
                        LOGGER.warn("[CustomDartClientCodegen] WARNING: ResponseWrapper*Data model NOT FILTERED: " + model.classname + 
                                   " - This should have been removed!");
                    }
                    filtered.add(mm);
                }
            }
            modelMaps.clear();
            modelMaps.addAll(filtered);
        } catch (Exception e) {
            LOGGER.warn("[CustomDartClientCodegen] Could not postProcessModels for response wrappers: " + e.getMessage());
        }
        return modelsMap;
    }
}
