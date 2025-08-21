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
import com.application.common.web.ResponseWrapper;

/**
 * Test di reflection per verificare la corrispondenza tra l'annotazione @WrapperType
 * e il tipo generico usato in ResponseWrapper<T> nei metodi dei controller.
 * Il test verifica che il dataClass corrisponda al tipo T e che il WrapperDataType
 * sia appropriato (DTO per oggetti singoli, LIST per List<T>, PAGE per Page<T>).
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
        // Test specifico per metodi che usano ResponseWrapper<List<T>> e ResponseWrapper<Page<T>>
        try {
            // Test AdminRestaurantReservationController con ResponseWrapper<List<T>>
            Class<?> adminRestaurantReservationController = Class.forName("com.application.admin.controller.restaurant.AdminRestaurantReservationController");
            
            Method getReservationsMethod = adminRestaurantReservationController.getDeclaredMethod("getReservations", 
                    Long.class, java.time.LocalDate.class, java.time.LocalDate.class);
            validateMethodSignature(adminRestaurantReservationController, getReservationsMethod);
            
            // Test metodo con ResponseWrapper<Page<T>>
            Method getReservationsPageableMethod = adminRestaurantReservationController.getDeclaredMethod("getReservationsPageable", 
                    Long.class, java.time.LocalDate.class, java.time.LocalDate.class, int.class, int.class);
            validateMethodSignature(adminRestaurantReservationController, getReservationsPageableMethod);
            
            // Test AdminServicesController con ResponseWrapper<List<T>>
            Class<?> adminServicesController = Class.forName("com.application.admin.controller.AdminServicesController");
            Method getServiceTypesMethod = adminServicesController.getDeclaredMethod("getServiceTypes");
            validateMethodSignature(adminServicesController, getServiceTypesMethod);
            
            System.out.println("✅ Controller con ResponseWrapper<List<T>> e ResponseWrapper<Page<T>> validati con successo");
            
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
            return; // Ignora metodi non parametrizzati
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
        
        // Verifica che il parametro di ResponseEntity sia ResponseWrapper
        Type responseBodyType = responseEntityArgs[0];
        if (!(responseBodyType instanceof ParameterizedType)) {
            return; // Ignora se non è parametrizzato
        }
        
        ParameterizedType wrapperType = (ParameterizedType) responseBodyType;
        Class<?> rawWrapperType = (Class<?>) wrapperType.getRawType();
        
        // Deve essere ResponseWrapper
        if (rawWrapperType != ResponseWrapper.class) {
            return; // Ignora se non è ResponseWrapper
        }
        
        // Estrai il tipo T dal ResponseWrapper<T>
        Type[] wrapperArgs = wrapperType.getActualTypeArguments();
        if (wrapperArgs.length != 1) {
            fail(String.format("ResponseWrapper deve avere esattamente un parametro generico in %s.%s", 
                    controllerClass.getSimpleName(), method.getName()));
            return;
        }
        
        Type actualWrapperTypeArg = wrapperArgs[0];
        
        // Determina il tipo di contenuto e la classe dati
        WrapperDataType expectedWrapperDataType;
        Class<?> expectedDataClass;
        String contentDescription;
        
        if (actualWrapperTypeArg instanceof Class) {
            // ResponseWrapper<SomeClass> - DTO
            expectedWrapperDataType = WrapperDataType.DTO;
            expectedDataClass = (Class<?>) actualWrapperTypeArg;
            contentDescription = expectedDataClass.getSimpleName();
        } else if (actualWrapperTypeArg instanceof ParameterizedType) {
            ParameterizedType parameterizedArg = (ParameterizedType) actualWrapperTypeArg;
            Class<?> rawType = (Class<?>) parameterizedArg.getRawType();
            
            if (rawType == java.util.List.class) {
                // ResponseWrapper<List<SomeClass>> - LIST
                expectedWrapperDataType = WrapperDataType.LIST;
                Type[] listArgs = parameterizedArg.getActualTypeArguments();
                if (listArgs.length != 1 || !(listArgs[0] instanceof Class)) {
                    fail(String.format("List deve avere esattamente un parametro di tipo Class in %s.%s", 
                            controllerClass.getSimpleName(), method.getName()));
                    return;
                }
                expectedDataClass = (Class<?>) listArgs[0];
                contentDescription = "List<" + expectedDataClass.getSimpleName() + ">";
            } else if (rawType.getSimpleName().equals("Page")) {
                // ResponseWrapper<Page<SomeClass>> - PAGE
                expectedWrapperDataType = WrapperDataType.PAGE;
                Type[] pageArgs = parameterizedArg.getActualTypeArguments();
                if (pageArgs.length != 1 || !(pageArgs[0] instanceof Class)) {
                    fail(String.format("Page deve avere esattamente un parametro di tipo Class in %s.%s", 
                            controllerClass.getSimpleName(), method.getName()));
                    return;
                }
                expectedDataClass = (Class<?>) pageArgs[0];
                contentDescription = "Page<" + expectedDataClass.getSimpleName() + ">";
            } else {
                // Altri tipi parametrizzati - tratta come DTO
                expectedWrapperDataType = WrapperDataType.DTO;
                expectedDataClass = rawType;
                contentDescription = rawType.getSimpleName() + "<...>";
            }
        } else if (actualWrapperTypeArg instanceof WildcardType) {
            // Gestisci wildcard types come ? extends SomeClass
            WildcardType wildcardType = (WildcardType) actualWrapperTypeArg;
            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds.length > 0 && upperBounds[0] instanceof Class) {
                expectedWrapperDataType = WrapperDataType.DTO;
                expectedDataClass = (Class<?>) upperBounds[0];
                contentDescription = "? extends " + expectedDataClass.getSimpleName();
            } else {
                expectedWrapperDataType = WrapperDataType.DTO;
                expectedDataClass = Object.class;
                contentDescription = "?";
            }
        } else {
            fail(String.format("Tipo non supportato per il parametro generico in %s.%s: %s", 
                    controllerClass.getSimpleName(), method.getName(), actualWrapperTypeArg.getClass()));
            return;
        }
        
        // Verifica la presenza dell'annotazione @WrapperType
        WrapperType annotation = method.getAnnotation(WrapperType.class);
        
        // REGOLA SPECIALE: ResponseWrapper<String> senza annotazione è accettabile per metodi void
        if (expectedDataClass == String.class && expectedWrapperDataType == WrapperDataType.DTO) {
            if (annotation == null) {
                System.out.printf("⚪ %s.%s - ResponseWrapper<String> senza annotazione (presumibilmente void operation)%n",
                        controllerClass.getSimpleName(), method.getName());
                return;
            }
        }
        
        assertNotNull(annotation, String.format(
                "Il metodo %s.%s che ritorna ResponseEntity<ResponseWrapper<%s>> deve avere l'annotazione @WrapperType",
                controllerClass.getSimpleName(), method.getName(), contentDescription
        ));
        
        // Verifica che il dataClass dell'annotazione corrisponda alla classe dati estratta
        Class<?> annotatedDataClass = annotation.dataClass();
        assertEquals(expectedDataClass, annotatedDataClass, String.format(
                "Mismatch in %s.%s: @WrapperType(dataClass=%s) non corrisponde al tipo dati estratto %s da ResponseWrapper<%s>",
                controllerClass.getSimpleName(), method.getName(),
                annotatedDataClass.getSimpleName(), expectedDataClass.getSimpleName(), contentDescription
        ));
        
        // Verifica che il tipo di wrapper nell'annotazione corrisponda al contenuto
        WrapperDataType annotatedWrapperType = annotation.type();
        assertEquals(expectedWrapperDataType, annotatedWrapperType, String.format(
                "Mismatch wrapper type in %s.%s: @WrapperType(type=%s) non corrisponde al tipo atteso %s per ResponseWrapper<%s>",
                controllerClass.getSimpleName(), method.getName(),
                annotatedWrapperType, expectedWrapperDataType, contentDescription
        ));
        
        System.out.printf("✅ %s.%s - Annotazione corretta: @WrapperType(dataClass=%s, type=%s) ↔ ResponseWrapper<%s>%n",
                controllerClass.getSimpleName(), method.getName(),
                annotatedDataClass.getSimpleName(), annotatedWrapperType, contentDescription);
    }
    
    @Test
    public void testMethodsWithoutWrapperTypeAnnotation() {
        // Test per verificare che i metodi senza ResponseWrapper non abbiano @WrapperType
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
                
                // Se il metodo non ritorna ResponseWrapper, non dovrebbe avere @WrapperType
                if (!returnsAnyWrapper(method) && method.isAnnotationPresent(WrapperType.class)) {
                    fail(String.format(
                            "Il metodo %s.%s non ritorna ResponseWrapper ma ha l'annotazione @WrapperType",
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
        
        return rawType == ResponseWrapper.class;
    }
}
