package com.application.common.spring.swagger.helper;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.application.common.spring.swagger.OperationAnalysis;
import com.application.common.spring.swagger.ResponseInfo;
import com.application.common.spring.swagger.ReturnTypeFlow;
import com.application.common.spring.swagger.WrapperTypeInfo;
import com.application.common.web.ResponseWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Engine per l'analisi statica dei metodi controller
 * 
 * Analizza i metodi dei controller per inferire:
 * - Pattern execute*() → HTTP status codes
 * - Method signatures → Wrapper types  
 * - Annotations → HTTP methods e error responses
 */
@Component
@Slf4j
public class StaticAnalysisEngine {
    
    /**
     * Analizza un metodo controller per estrarre tutte le informazioni OpenAPI
     */
    public OperationAnalysis analyzeMethod(Method method) {
        // Track AuthResponseDTO specifically
        String returnTypeString = method.getReturnType().getGenericSuperclass() != null ? 
            method.getReturnType().getGenericSuperclass().toString() : 
            method.getReturnType().toString();
        boolean isAuthResponseMethod = returnTypeString.contains("AuthResponseDTO") || 
                                       method.getName().contains("auth") || 
                                       method.getName().contains("Auth") ||
                                       method.getName().contains("login") ||
                                       method.getName().contains("refresh");
        
        if (isAuthResponseMethod) {
            System.out.println("🔍 AuthResponseDTO: StaticAnalysisEngine analyzing method " + 
                method.getDeclaringClass().getSimpleName() + "." + method.getName());
        }
        
        //log.debug("🔍 STATIC_ANALYSIS: Analyzing {}.{}", 
        //    method.getDeclaringClass().getSimpleName(), method.getName());
        
        try {
            String methodSignature = buildMethodSignature(method);
            ReturnTypeFlow flow = analyzeReturnTypeFlow(method);
            String httpMethod = detectHttpMethodFromAnnotations(method);
            WrapperTypeInfo wrapperTypeInfo = detectWrapperTypeFromMethod(method);
            
            List<ResponseInfo> successResponses = inferSuccessResponses(flow, wrapperTypeInfo);
            List<ResponseInfo> errorResponses = inferErrorResponses(method, wrapperTypeInfo);
            
            OperationAnalysis analysis = OperationAnalysis.builder()
                .returnTypeFlow(flow)
                .httpMethod(httpMethod)
                .wrapperTypeInfo(wrapperTypeInfo)
                .successResponses(successResponses)
                .errorResponses(errorResponses)
                .methodSignature(methodSignature)
                .analysisComplete(flow != ReturnTypeFlow.UNKNOWN && wrapperTypeInfo != null)
                .notes(buildAnalysisNotes(flow, wrapperTypeInfo))
                .build();
            
            //log.debug("✅ STATIC_ANALYSIS: Completed analysis for {} - flow: {}, wrapper: {}", 
            //    methodSignature, flow, wrapperTypeInfo != null ? wrapperTypeInfo.getWrapperSchemaName() : "null");
            
            return analysis;
            
        } catch (Exception e) {
            //log.error("❌ STATIC_ANALYSIS: Error analyzing method {}.{}: {}", 
            //    method.getDeclaringClass().getSimpleName(), method.getName(), e.getMessage());
            
            return OperationAnalysis.builder()
                .methodSignature(buildMethodSignature(method))
                .returnTypeFlow(ReturnTypeFlow.UNKNOWN)
                .analysisComplete(false)
                .notes("Analysis failed: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Analizza il corpo del metodo per rilevare pattern execute*()
     */
    private ReturnTypeFlow analyzeReturnTypeFlow(Method method) {
        // String methodName = method.getName(); // Not used when logs are commented
        Type returnType = method.getGenericReturnType();
        
        // ✅ PRIORITÀ 1: Analisi del codice sorgente (più accurato)
        ReturnTypeFlow sourceCodeFlow = analyzeSourceCodeForExecutePatterns(method);
        if (sourceCodeFlow != ReturnTypeFlow.UNKNOWN) {
            //log.debug("🎯 STATIC_ANALYSIS: Source code analysis succeeded for {} → {}", 
            //    methodName, sourceCodeFlow);
            return sourceCodeFlow;
        }
        
        // ✅ PRIORITÀ 2: Pattern method name migliorato (fallback più specifico)
        ReturnTypeFlow methodNameFlow = analyzeMethodNamePatterns(method);
        if (methodNameFlow != ReturnTypeFlow.UNKNOWN) {
            //log.warn("⚠️ STATIC_ANALYSIS: Using method name fallback for {} → {}", 
            //    methodName, methodNameFlow);
            return methodNameFlow;
        }
        
        // Pattern 3: Analisi del return type per determinare il tipo di response
        if (returnType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) returnType;
            if (paramType.getRawType().equals(ResponseEntity.class)) {
                Type[] args = paramType.getActualTypeArguments();
                if (args.length > 0 && args[0] instanceof ParameterizedType) {
                    ParameterizedType wrapperType = (ParameterizedType) args[0];
                    if (wrapperType.getRawType().equals(ResponseWrapper.class)) {
                        Type[] wrapperArgs = wrapperType.getActualTypeArguments();
                        if (wrapperArgs.length > 0) {
                            
                            // Controlla se il data type è Page<T> → PAGINATED
                            if (wrapperArgs[0] instanceof ParameterizedType) {
                                ParameterizedType dataType = (ParameterizedType) wrapperArgs[0];
                                if (dataType.getRawType().equals(Page.class)) {
                                    return ReturnTypeFlow.READ_200_PAGINATED;
                                }
                                if (dataType.getRawType().equals(List.class)) {
                                    return ReturnTypeFlow.READ_200_LIST;
                                }
                                // Altri ParameterizedType → SINGLE
                                return ReturnTypeFlow.READ_200_SINGLE;
                            }
                            // Controlla se il data type è String → VOID
                            else if (wrapperArgs[0].equals(String.class)) {
                                return ReturnTypeFlow.READ_200_VOID;
                            }
                            // Single object
                            else {
                                return ReturnTypeFlow.READ_200_SINGLE;
                            }
                        }
                    }
                }
            }
        }
        
        // ✅ PRIORITÀ 3: Fallback finale - assume READ operation  
        //log.debug("⚠️ STATIC_ANALYSIS: All patterns failed for {}, defaulting to READ operation", methodName);
        return ReturnTypeFlow.READ_200_SINGLE;
    }
    
    /**
     * rileva HTTP method dalle annotazioni Spring
     */
    private String detectHttpMethodFromAnnotations(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) return "GET";
        if (method.isAnnotationPresent(PostMapping.class)) return "POST";
        if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        if (method.isAnnotationPresent(PatchMapping.class)) return "PATCH";
        
        // Fallback su @RequestMapping
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping.method().length > 0) {
                return mapping.method()[0].name();
            }
        }
        
        return "GET"; // Default fallback
    }
    
    /**
     * Rileva wrapper type dalla method signature (stesso codice di WrapperTypeOperationCustomizer)
     */
    private WrapperTypeInfo detectWrapperTypeFromMethod(Method method) {
        Type returnType = method.getGenericReturnType();
        
        // Check if return type is ResponseEntity<ResponseWrapper<T>>
        if (returnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) returnType;
            
            // First level: ResponseEntity<X>
            if (parameterizedType.getRawType().equals(ResponseEntity.class)) {
                Type[] responseEntityArgs = parameterizedType.getActualTypeArguments();
                if (responseEntityArgs.length > 0 && responseEntityArgs[0] instanceof ParameterizedType) {
                    ParameterizedType wrapperType = (ParameterizedType) responseEntityArgs[0];
                    
                    // Second level: ResponseWrapper<Y>
                    if (wrapperType.getRawType().equals(ResponseWrapper.class)) {
                        Type[] wrapperArgs = wrapperType.getActualTypeArguments();
                        if (wrapperArgs.length > 0) {
                            return analyzeWrapperDataType(wrapperArgs[0]);
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Analizza il data type dentro ResponseWrapper (stesso codice di WrapperTypeOperationCustomizer)
     */
    private WrapperTypeInfo analyzeWrapperDataType(Type dataType) {
        if (dataType instanceof Class<?>) {
            Class<?> dataClass = (Class<?>) dataType;
            return new WrapperTypeInfo(dataClass.getName(), "DTO", "Single object response");
            
        } else if (dataType instanceof ParameterizedType) {
            ParameterizedType parameterizedDataType = (ParameterizedType) dataType;
            Class<?> rawType = (Class<?>) parameterizedDataType.getRawType();
            
            if (rawType.equals(List.class)) {
                Type[] listArgs = parameterizedDataType.getActualTypeArguments();
                if (listArgs.length > 0 && listArgs[0] instanceof Class<?>) {
                    Class<?> listElementClass = (Class<?>) listArgs[0];
                    return new WrapperTypeInfo(listElementClass.getName(), "LIST", "List response");
                }
                
            } else if (rawType.equals(Page.class)) {
                Type[] pageArgs = parameterizedDataType.getActualTypeArguments();
                if (pageArgs.length > 0 && pageArgs[0] instanceof Class<?>) {
                    Class<?> pageElementClass = (Class<?>) pageArgs[0];
                    return new WrapperTypeInfo(pageElementClass.getName(), "PAGE", "Paginated response");
                }
            }
        }
        
        return null;
    }
    
    /**
     * Inferisce le success responses basandosi sul flow
     */
    private List<ResponseInfo> inferSuccessResponses(ReturnTypeFlow flow, WrapperTypeInfo wrapperTypeInfo) {
        List<ResponseInfo> responses = new ArrayList<>();
        
        if (wrapperTypeInfo == null) {
            return responses; // Non possiamo creare response senza wrapper info
        }
        
        String wrapperSchemaRef = wrapperTypeInfo.getWrapperSchemaReference();
        String wrapperType = wrapperTypeInfo.wrapperType;
        
        switch (flow) {
            case CREATE_201:
                responses.add(ResponseInfo.success("201", "Created successfully", wrapperSchemaRef, wrapperType));
                break;
            case READ_200_SINGLE:
                responses.add(ResponseInfo.success("200", "Operation completed successfully", wrapperSchemaRef, wrapperType));
                break;
            case READ_200_LIST:
                responses.add(ResponseInfo.success("200", "List retrieved successfully", wrapperSchemaRef, wrapperType));
                break;
            case READ_200_PAGINATED:
                responses.add(ResponseInfo.success("200", "Paginated results retrieved successfully", wrapperSchemaRef, wrapperType));
                break;
            case READ_200_VOID:
                responses.add(ResponseInfo.success("200", "Operation completed successfully", wrapperSchemaRef, wrapperType));
                break;
            case UNKNOWN:
                // Non aggiungiamo success response se il pattern non è riconosciuto
                break;
        }
        
        return responses;
    }
    
    /**
     * Inferisce le error responses dalle annotazioni del metodo
     * 
     * NOTA: Per ora creiamo le error response standard. 
     * In futuro si potrebbe analizzare le annotazioni @ReadApiResponses, @CreateApiResponses
     */
    private List<ResponseInfo> inferErrorResponses(Method method, WrapperTypeInfo wrapperTypeInfo) {
        List<ResponseInfo> responses = new ArrayList<>();
        
        if (wrapperTypeInfo == null) {
            return responses;
        }
        
        // FIXED: Usa schema consolidati oneOf invece di schema generici sbagliati
        String errorSchemaRef = "#/components/schemas/ResponseWrapperSingle"; // Per errori usiamo sempre Single con oneOf
        
        responses.add(ResponseInfo.error("400", "Bad Request", errorSchemaRef));
        responses.add(ResponseInfo.error("401", "Unauthorized", errorSchemaRef));
        responses.add(ResponseInfo.error("403", "Forbidden", errorSchemaRef));
        responses.add(ResponseInfo.error("500", "Internal Server Error", errorSchemaRef));
        
        return responses;
    }
    
    /**
     * Analizza il codice sorgente del metodo per rilevare chiamate execute*()
     * 
     * ANALISI STATICA: Cerca pattern nel codice sorgente:
     * - executeCreate() → CREATE_201
     * - execute() → READ_200_SINGLE  
     * - executeVoid() → READ_200_VOID
     * - executeList() → READ_200_LIST
     * - executePaginated() → READ_200_PAGINATED
     */
    private ReturnTypeFlow analyzeSourceCodeForExecutePatterns(Method method) {
        try {
            String sourceCode = getMethodSourceCode(method);
            if (sourceCode == null || sourceCode.isEmpty()) {
                return ReturnTypeFlow.UNKNOWN;
            }
            
            // Pattern per executeCreate
            if (sourceCode.contains("executeCreate(")) {
                //log.debug("🔍 STATIC_ANALYSIS: Found executeCreate() call in {}", method.getName());
                return ReturnTypeFlow.CREATE_201;
            }
            
            // Pattern per executeVoid
            if (sourceCode.contains("executeVoid(")) {
                //log.debug("🔍 STATIC_ANALYSIS: Found executeVoid() call in {}", method.getName());
                return ReturnTypeFlow.READ_200_VOID;
            }
            
            // Pattern per executeList
            if (sourceCode.contains("executeList(")) {
                //log.debug("🔍 STATIC_ANALYSIS: Found executeList() call in {}", method.getName());
                return ReturnTypeFlow.READ_200_LIST;
            }
            
            // Pattern per executePaginated
            if (sourceCode.contains("executePaginated(")) {
                //log.debug("🔍 STATIC_ANALYSIS: Found executePaginated() call in {}", method.getName());
                return ReturnTypeFlow.READ_200_PAGINATED;
            }
            
            // Pattern per execute generico
            if (sourceCode.contains("execute(")) {
                //log.debug("🔍 STATIC_ANALYSIS: Found execute() call in {}", method.getName());
                return ReturnTypeFlow.READ_200_SINGLE;
            }
            
            return ReturnTypeFlow.UNKNOWN;
            
        } catch (Exception e) {
            //log.warn("⚠️ STATIC_ANALYSIS: Could not analyze source code for {}: {}", 
            //    method.getName(), e.getMessage());
            return ReturnTypeFlow.UNKNOWN;
        }
    }
    
    /**
     * Ottiene il codice sorgente del metodo analizzando il file .java
     * 
     * STRATEGIA:
     * 1. Trova il file sorgente della classe
     * 2. Legge il contenuto del file
     * 3. Estrae il codice del metodo specifico
     */
    private String getMethodSourceCode(Method method) {
        try {
            Class<?> clazz = method.getDeclaringClass();
            String className = clazz.getSimpleName();
            String packageName = clazz.getPackageName();
            
            // Trova il file sorgente
            String sourceFilePath = findSourceFile(packageName, className);
            if (sourceFilePath == null) {
                return null;
            }
            
            // Legge il contenuto del file
            String fileContent = Files.readString(Paths.get(sourceFilePath));
            
            // Estrae il codice del metodo
            return extractMethodBody(fileContent, method.getName());
            
        } catch (Exception e) {
            //log.debug("🔍 STATIC_ANALYSIS: Could not read source code for {}.{}: {}", 
            //    method.getDeclaringClass().getSimpleName(), method.getName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Trova il file sorgente della classe nel filesystem
     */
    private String findSourceFile(String packageName, String className) {
        try {
            // Prova nel percorso standard src/main/java
            String relativePath = packageName.replace('.', File.separatorChar) + File.separatorChar + className + ".java";
            
            // Cerca dalla directory corrente
            String projectRoot = System.getProperty("user.dir");
            String[] possiblePaths = {
                projectRoot + File.separatorChar + "src" + File.separatorChar + "main" + File.separatorChar + "java" + File.separatorChar + relativePath,
                projectRoot + File.separatorChar + "greedys_api" + File.separatorChar + "src" + File.separatorChar + "main" + File.separatorChar + "java" + File.separatorChar + relativePath
            };
            
            for (String path : possiblePaths) {
                File file = new File(path);
                if (file.exists() && file.isFile()) {
                    return file.getAbsolutePath();
                }
            }
            
            return null;
            
        } catch (Exception e) {
            //log.debug("🔍 STATIC_ANALYSIS: Error finding source file for {}: {}", className, e.getMessage());
            return null;
        }
    }
    
    /**
     * Estrae il corpo del metodo dal codice sorgente
     * 
     * SEMPLIFICAZIONE: Cerca il metodo e prende tutto fino al prossimo metodo pubblico
     * Non è un parser completo ma funziona per la maggior parte dei casi
     */
    private String extractMethodBody(String fileContent, String methodName) {
        try {
            int startIndex = fileContent.indexOf(methodName + "(");
            if (startIndex == -1) {
                return null;
            }
            
            // Trova l'inizio del corpo del metodo (prima parentesi graffa)
            int openBraceIndex = fileContent.indexOf('{', startIndex);
            if (openBraceIndex == -1) {
                return null;
            }
            
            // Trova la fine del corpo del metodo contando le parentesi graffe
            int braceCount = 1;
            int currentIndex = openBraceIndex + 1;
            
            while (currentIndex < fileContent.length() && braceCount > 0) {
                char c = fileContent.charAt(currentIndex);
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                }
                currentIndex++;
            }
            
            if (braceCount == 0) {
                return fileContent.substring(openBraceIndex + 1, currentIndex - 1).trim();
            }
            
            return null;
            
        } catch (Exception e) {
            //log.debug("🔍 STATIC_ANALYSIS: Error extracting method body for {}: {}", methodName, e.getMessage());
            return null;
        }
    }
    
    /**
     * ✅ NEW: Analisi pattern metodo più specifici per evitare false positives
     * Sostituisce il vecchio Pattern 1 con logica più precisa
     */
    private ReturnTypeFlow analyzeMethodNamePatterns(Method method) {
        String methodName = method.getName();
        
        // ✅ Pattern CREATE: Solo metodi che INIZIANO con parole specifiche
        if (methodName.startsWith("create") || methodName.startsWith("add") || 
            methodName.startsWith("register") || methodName.startsWith("new")) {
            //log.debug("🎯 METHOD_PATTERN: CREATE detected for {}", methodName);
            return ReturnTypeFlow.CREATE_201;
        }
        
        // ✅ Pattern READ: Metodi che iniziano con verbi di lettura
        if (methodName.startsWith("get") || methodName.startsWith("find") || 
            methodName.startsWith("retrieve") || methodName.startsWith("fetch")) {
            //log.debug("🎯 METHOD_PATTERN: READ detected for {}", methodName);
            return ReturnTypeFlow.READ_200_SINGLE;
        }
        
        // ✅ Pattern UPDATE: Metodi che iniziano con verbi di modifica
        if (methodName.startsWith("update") || methodName.startsWith("modify") || 
            methodName.startsWith("edit") || methodName.startsWith("change")) {
            //log.debug("🎯 METHOD_PATTERN: UPDATE detected for {}", methodName);
            return ReturnTypeFlow.READ_200_SINGLE;
        }
        
        // ✅ Pattern DELETE/VOID: Metodi che iniziano con verbi di eliminazione
        if (methodName.startsWith("delete") || methodName.startsWith("remove") || 
            methodName.startsWith("cancel")) {
            //log.debug("🎯 METHOD_PATTERN: DELETE detected for {}", methodName);
            return ReturnTypeFlow.READ_200_VOID;
        }
        
        // ✅ Pattern specifici per azioni che dovrebbero essere 200, non 201
        if (methodName.startsWith("accept") || methodName.startsWith("reject") || 
            methodName.startsWith("mark") || methodName.startsWith("set") ||
            methodName.startsWith("confirm") || methodName.startsWith("resend")) {
            //log.debug("🎯 METHOD_PATTERN: ACTION detected for {}", methodName);
            return ReturnTypeFlow.READ_200_SINGLE;
        }
        
        //log.debug("⚠️ METHOD_PATTERN: No pattern matched for {}", methodName);
        return ReturnTypeFlow.UNKNOWN;
    }
    
    /**
     * Costruisce signature univoca del metodo
     */
    private String buildMethodSignature(Method method) {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }
    
    /**
     * Costruisce note per l'analisi
     */
    private String buildAnalysisNotes(ReturnTypeFlow flow, WrapperTypeInfo wrapperTypeInfo) {
        StringBuilder notes = new StringBuilder();
        
        if (flow == ReturnTypeFlow.UNKNOWN) {
            notes.append("Unknown return type flow. ");
        }
        
        if (wrapperTypeInfo == null) {
            notes.append("No wrapper type detected. ");
        }
        
        return notes.length() > 0 ? notes.toString().trim() : "Analysis completed successfully";
    }
}
