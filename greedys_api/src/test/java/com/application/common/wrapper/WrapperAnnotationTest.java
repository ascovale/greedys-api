package com.application.common.wrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.application.common.controller.annotation.WrapperDataType;
import com.application.common.controller.annotation.WrapperType;
import com.application.common.web.ListResponseWrapper;
import com.application.common.web.PageResponseWrapper;
import com.application.common.web.ResponseWrapper;

/**
 * Test di reflection per verificare la corrispondenza tra l'annotazione @WrapperType
 * e il tipo generico usato in ResponseWrapper<T>, ListResponseWrapper<T> e PageResponseWrapper<T> nei metodi dei controller.
 */
public class WrapperAnnotationTest {

    @Test
    public void testWrapperAnnotationsOnAllControllerMethods() {
        // Scansiona i controller nei package specifici dell'applicazione
        String[] controllerPackages = {
            "com.application.admin.controller",
            "com.application.restaurant.controller", 
            "com.application.customer.controller"
        };
        
        int methodsValidated = 0;
        Set<Class<?>> allControllerClasses = new HashSet<>();
        java.util.List<String> allErrors = new java.util.ArrayList<>();
        
        for (String packageName : controllerPackages) {
            Reflections reflections = new Reflections(packageName, Scanners.TypesAnnotated);
            Set<Class<?>> controllerClasses = reflections.getTypesAnnotatedWith(RestController.class);
            allControllerClasses.addAll(controllerClasses);
        }
        
        assertFalse(allControllerClasses.isEmpty(), "Dovrebbero essere presenti dei controller annotati con @RestController");
        
        for (Class<?> controllerClass : allControllerClasses) {
            methodsValidated += validateControllerMethods(controllerClass, allErrors);
        }
        
        // Se ci sono errori, li mostriamo tutti insieme
        if (!allErrors.isEmpty()) {
            System.err.println("\n❌ ERRORI TROVATI:");
            for (int i = 0; i < allErrors.size(); i++) {
                System.err.printf("%d. %s%n", i + 1, allErrors.get(i));
            }
            fail(String.format("Trovati %d errori di validazione (vedi output sopra)", allErrors.size()));
        }
        
        System.out.printf("✅ Test completato: %d metodi validati con successo in %d controller%n", methodsValidated, allControllerClasses.size());
    }
    
    @Test
    public void testAdminReservationControllerSpecific() {
        // Test specifico per AdminReservationController
        try {
            Class<?> adminReservationController = Class.forName("com.application.admin.controller.AdminReservationController");
            
            // Test del metodo getReservation
            Method getReservationMethod = adminReservationController.getDeclaredMethod("getReservation", Long.class);
            validateMethodSignature(adminReservationController, getReservationMethod);
            
            // Test del metodo modifyReservation
            Class<?> adminNewReservationDTO = Class.forName("com.application.admin.web.dto.reservation.AdminNewReservationDTO");
            Method modifyReservationMethod = adminReservationController.getDeclaredMethod("modifyReservation", 
                    Long.class, adminNewReservationDTO);
            validateMethodSignature(adminReservationController, modifyReservationMethod);
            
            // Test del metodo createReservation
            Method createReservationMethod = adminReservationController.getDeclaredMethod("createReservation", adminNewReservationDTO);
            validateMethodSignature(adminReservationController, createReservationMethod);
            
            System.out.println("✅ AdminReservationController validato con successo");
            
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            fail("Impossibile trovare la classe o il metodo per il test specifico: " + e.getMessage());
        }
    }
    
    @Test
    public void testListAndPageWrapperControllers() {
        // Test specifico per metodi che usano ListResponseWrapper e PageResponseWrapper
        try {
            // Test AdminRestaurantReservationController con ListResponseWrapper
            Class<?> adminRestaurantReservationController = Class.forName("com.application.admin.controller.restaurant.AdminRestaurantReservationController");
            
            Method getReservationsMethod = adminRestaurantReservationController.getDeclaredMethod("getReservations", 
                    Long.class, java.time.LocalDate.class, java.time.LocalDate.class);
            validateMethodSignature(adminRestaurantReservationController, getReservationsMethod);
            
            // Test metodo con PageResponseWrapper
            Method getReservationsPageableMethod = adminRestaurantReservationController.getDeclaredMethod("getReservationsPageable", 
                    Long.class, java.time.LocalDate.class, java.time.LocalDate.class, int.class, int.class);
            validateMethodSignature(adminRestaurantReservationController, getReservationsPageableMethod);
            
            // Test AdminServicesController con ListResponseWrapper
            Class<?> adminServicesController = Class.forName("com.application.admin.controller.AdminServicesController");
            Method getServiceTypesMethod = adminServicesController.getDeclaredMethod("getServiceTypes");
            validateMethodSignature(adminServicesController, getServiceTypesMethod);
            
            System.out.println("✅ Controller con ListResponseWrapper e PageResponseWrapper validati con successo");
            
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            fail("Impossibile trovare la classe o il metodo per il test specifico: " + e.getMessage());
        }
    }
    
    private int validateControllerMethods(Class<?> controllerClass, java.util.List<String> errors) {
        Method[] methods = controllerClass.getDeclaredMethods();
        int validatedCount = 0;
        
        for (Method method : methods) {
            // Controlla solo i metodi pubblici che hanno mapping annotations
            if (!isControllerMethod(method)) {
                continue;
            }
            
            try {
                validateMethodSignature(controllerClass, method);
                validatedCount++;
            } catch (AssertionError e) {
                errors.add(String.format("%s.%s: %s", controllerClass.getSimpleName(), method.getName(), e.getMessage()));
            }
        }
        
        return validatedCount;
    }
    
    private boolean isControllerMethod(Method method) {
        // Verifica se il metodo è pubblico e ha annotazioni di mapping
        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }
        
        return method.isAnnotationPresent(RequestMapping.class) ||
               method.isAnnotationPresent(GetMapping.class) ||
               method.isAnnotationPresent(PostMapping.class) ||
               method.isAnnotationPresent(PutMapping.class) ||
               method.isAnnotationPresent(DeleteMapping.class) ||
               method.isAnnotationPresent(PatchMapping.class);
    }
    
    private void validateMethodSignature(Class<?> controllerClass, Method method) {
        Type genericReturnType = method.getGenericReturnType();
        
        // Verifica che il tipo di ritorno sia parametrizzato
        if (!(genericReturnType instanceof ParameterizedType)) {
            // Ignora metodi che non hanno tipi generici (es. void methods)
            return;
        }
        
        ParameterizedType returnType = (ParameterizedType) genericReturnType;
        
        // Verifica che sia ResponseEntity
        if (returnType.getRawType() != ResponseEntity.class) {
            return; // Ignora metodi che non ritornano ResponseEntity
        }
        
        Type[] responseEntityArgs = returnType.getActualTypeArguments();
        if (responseEntityArgs.length != 1) {
            fail(String.format("ResponseEntity deve avere esattamente un parametro generico in %s.%s", 
                    controllerClass.getSimpleName(), method.getName()));
            return;
        }
        
        // Verifica che il parametro di ResponseEntity sia un wrapper (ResponseWrapper, ListResponseWrapper, PageResponseWrapper)
        Type responseBodyType = responseEntityArgs[0];
        if (!(responseBodyType instanceof ParameterizedType)) {
            return; // Ignora se non è parametrizzato (es. ResponseEntity<String> senza wrapper)
        }
        
        ParameterizedType wrapperType = (ParameterizedType) responseBodyType;
        Class<?> rawWrapperType = (Class<?>) wrapperType.getRawType();
        
        // Determina il tipo di wrapper e l'annotazione attesa
        WrapperDataType expectedWrapperDataType;
        String wrapperName;
        
        if (rawWrapperType == ResponseWrapper.class) {
            expectedWrapperDataType = WrapperDataType.DTO;
            wrapperName = "ResponseWrapper";
        } else if (rawWrapperType == ListResponseWrapper.class) {
            expectedWrapperDataType = WrapperDataType.LIST;
            wrapperName = "ListResponseWrapper";
        } else if (rawWrapperType == PageResponseWrapper.class) {
            expectedWrapperDataType = WrapperDataType.PAGE;
            wrapperName = "PageResponseWrapper";
        } else {
            return; // Ignora se non è uno dei wrapper supportati
        }
        
        // Estrai il tipo T dal wrapper<T>
        Type[] wrapperArgs = wrapperType.getActualTypeArguments();
        if (wrapperArgs.length != 1) {
            fail(String.format("%s deve avere esattamente un parametro generico in %s.%s", 
                    wrapperName, controllerClass.getSimpleName(), method.getName()));
            return;
        }
        
        Type actualWrapperTypeArg = wrapperArgs[0];
        Class<?> expectedWrappedClass;
        
        // Gestisci diversi tipi di Type
        if (actualWrapperTypeArg instanceof Class) {
            expectedWrappedClass = (Class<?>) actualWrapperTypeArg;
        } else if (actualWrapperTypeArg instanceof ParameterizedType) {
            expectedWrappedClass = (Class<?>) ((ParameterizedType) actualWrapperTypeArg).getRawType();
        } else if (actualWrapperTypeArg instanceof WildcardType) {
            // Gestisci wildcard types come ? extends SomeClass
            WildcardType wildcardType = (WildcardType) actualWrapperTypeArg;
            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds.length > 0 && upperBounds[0] instanceof Class) {
                expectedWrappedClass = (Class<?>) upperBounds[0];
            } else {
                expectedWrappedClass = Object.class;
            }
        } else {
            fail(String.format("Tipo non supportato per il parametro generico in %s.%s: %s", 
                    controllerClass.getSimpleName(), method.getName(), actualWrapperTypeArg.getClass()));
            return;
        }
        
        // REGOLA SPECIALE: Ignora ResponseWrapper<String> senza annotazione (comportamento di default)
        // E anche ResponseWrapper<T> senza annotazione, considerando DTO come default
        if (rawWrapperType == ResponseWrapper.class) {
            WrapperType annotation = method.getAnnotation(WrapperType.class);
            if (annotation == null) {
                // ResponseWrapper senza annotazione è accettabile (default = DTO)
                System.out.printf("⚪ %s.%s - ResponseWrapper<%s> senza annotazione (default DTO OK)%n",
                        controllerClass.getSimpleName(), method.getName(), expectedWrappedClass.getSimpleName());
                return;
            } else if (annotation.dataClass() == String.class && 
                       annotation.type() == WrapperDataType.DTO && 
                       "200".equals(annotation.responseCode())) {
                // Annotazione ridondante ma non errore per ResponseWrapper<String>
                System.out.printf("⚪ %s.%s - Annotazione ridondante ma valida per ResponseWrapper<String>%n",
                        controllerClass.getSimpleName(), method.getName());
                return;
            }
            // Se ha responseCode diverso da "200" o dataClass diversa da quella attesa, continua la validazione normale
        }
        
        // Verifica la presenza dell'annotazione @WrapperType per tutti gli altri casi
        WrapperType annotation = method.getAnnotation(WrapperType.class);
        assertNotNull(annotation, String.format(
                "Il metodo %s.%s che ritorna ResponseEntity<%s<%s>> deve avere l'annotazione @WrapperType",
                controllerClass.getSimpleName(), method.getName(), wrapperName, expectedWrappedClass.getSimpleName()
        ));
        
        // Verifica che il dataClass dell'annotazione corrisponda al tipo T
        Class<?> annotatedDataClass = annotation.dataClass();
        assertEquals(expectedWrappedClass, annotatedDataClass, String.format(
                "Mismatch in %s.%s: @WrapperType(dataClass=%s) non corrisponde a %s<%s>",
                controllerClass.getSimpleName(), method.getName(),
                annotatedDataClass.getSimpleName(), wrapperName, expectedWrappedClass.getSimpleName()
        ));
        
        // Verifica che il tipo di wrapper nell'annotazione corrisponda al wrapper utilizzato
        // NOTA: Per ResponseWrapper, se type non è specificato, assume DTO come default
        WrapperDataType annotatedWrapperType = annotation.type();
        if (rawWrapperType == ResponseWrapper.class && annotatedWrapperType == WrapperDataType.DTO) {
            // Per ResponseWrapper, DTO è considerato default, quindi non è necessario specificarlo
            // Ma se è specificato esplicitamente, va bene comunque
        } else {
            assertEquals(expectedWrapperDataType, annotatedWrapperType, String.format(
                    "Mismatch wrapper type in %s.%s: @WrapperType(type=%s) non corrisponde a %s utilizzato",
                    controllerClass.getSimpleName(), method.getName(),
                    annotatedWrapperType, wrapperName
            ));
        }
        
        System.out.printf("✅ %s.%s - Annotazione corretta: @WrapperType(dataClass=%s, type=%s) ↔ %s<%s>%n",
                controllerClass.getSimpleName(), method.getName(),
                annotatedDataClass.getSimpleName(), annotatedWrapperType, wrapperName, expectedWrappedClass.getSimpleName());
    }
    
    @Test
    public void testMethodsWithoutWrapperTypeAnnotation() {
        // Test per verificare che i metodi senza wrapper non abbiano @WrapperType
        String[] controllerPackages = {
            "com.application.admin.controller",
            "com.application.restaurant.controller", 
            "com.application.customer.controller"
        };
        
        Set<Class<?>> allControllerClasses = new HashSet<>();
        
        for (String packageName : controllerPackages) {
            Reflections reflections = new Reflections(packageName, Scanners.TypesAnnotated);
            Set<Class<?>> controllerClasses = reflections.getTypesAnnotatedWith(RestController.class);
            allControllerClasses.addAll(controllerClasses);
        }
        
        for (Class<?> controllerClass : allControllerClasses) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (!isControllerMethod(method)) {
                    continue;
                }
                
                // Se il metodo non ritorna un wrapper, non dovrebbe avere @WrapperType
                if (!returnsAnyWrapper(method) && method.isAnnotationPresent(WrapperType.class)) {
                    fail(String.format(
                            "Il metodo %s.%s non ritorna un wrapper ma ha l'annotazione @WrapperType",
                            controllerClass.getSimpleName(), method.getName()
                    ));
                }
            }
        }
    }
    
    private boolean returnsAnyWrapper(Method method) {
        Type genericReturnType = method.getGenericReturnType();
        
        if (!(genericReturnType instanceof ParameterizedType)) {
            return false;
        }
        
        ParameterizedType returnType = (ParameterizedType) genericReturnType;
        
        if (returnType.getRawType() != ResponseEntity.class) {
            return false;
        }
        
        Type[] args = returnType.getActualTypeArguments();
        if (args.length != 1 || !(args[0] instanceof ParameterizedType)) {
            return false;
        }
        
        ParameterizedType responseBodyType = (ParameterizedType) args[0];
        Class<?> rawType = (Class<?>) responseBodyType.getRawType();
        
        return rawType == ResponseWrapper.class || 
               rawType == ListResponseWrapper.class || 
               rawType == PageResponseWrapper.class;
    }
}
