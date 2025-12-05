# @NotActive Annotation - Developer Tools

## Panoramica

L'annotazione `@NotActive` permette di disabilitare endpoint REST sia a livello di controller che di singolo metodo. Gli endpoint disabilitati:

- Restituiscono **404 Not Found** quando chiamati
- Sono **nascosti da Swagger/OpenAPI**
- Possono loggare i tentativi di accesso (configurabile)

## Struttura Package

```
com.application.common.devtools/
├── annotation/
│   └── NotActive.java              # Annotazione principale
├── config/
│   └── NotActiveConfiguration.java # Registrazione interceptor
├── interceptor/
│   └── NotActiveInterceptor.java   # Gestione richieste HTTP
├── swagger/
│   ├── NotActiveOperationCustomizer.java  # Nasconde singole operazioni
│   └── NotActiveOpenApiCustomizer.java    # Nasconde interi controller
└── example/
    └── NotActiveExampleController.java    # Esempio di utilizzo
```

## Utilizzo

### 1. Disabilitare un intero Controller

```java
@NotActive(reason = "Controller in sviluppo")
@RestController
@RequestMapping("/api/v1/feature")
public class MyFeatureController {
    
    @GetMapping("/endpoint1")
    public String endpoint1() { ... }  // Disabilitato
    
    @GetMapping("/endpoint2")
    public String endpoint2() { ... }  // Disabilitato
}
```

### 2. Disabilitare un singolo Endpoint

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @GetMapping
    public List<User> getUsers() { ... }  // Attivo
    
    @NotActive(reason = "Feature non ancora rilasciata", targetVersion = "2.1")
    @PostMapping("/bulk-import")
    public void bulkImport() { ... }  // Disabilitato
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) { ... }  // Attivo
}
```

### 3. Disabilitare senza Logging

```java
@NotActive(reason = "Deprecato", logAttempts = false)
@GetMapping("/old-endpoint")
public String oldEndpoint() { ... }
```

## Parametri Annotazione

| Parametro | Tipo | Default | Descrizione |
|-----------|------|---------|-------------|
| `reason` | String | `""` | Motivo della disattivazione |
| `targetVersion` | String | `""` | Versione target per riattivazione |
| `logAttempts` | boolean | `true` | Se loggare i tentativi di accesso |

## Comportamento

### Richiesta HTTP a Endpoint Disabilitato

```http
GET /api/v1/feature/disabled-endpoint HTTP/1.1
Host: localhost:8080
```

**Risposta:**
```json
HTTP/1.1 404 Not Found
Content-Type: application/json

{
    "error": "Not Found",
    "message": "The requested endpoint is not available",
    "status": 404,
    "path": "/api/v1/feature/disabled-endpoint"
}
```

### Log (se `logAttempts = true`)

```
WARN  c.a.c.d.i.NotActiveInterceptor - Access attempt to disabled endpoint: 
      GET /api/v1/feature/disabled-endpoint - Target: disabledEndpoint 
      (Reason: Feature non ancora rilasciata) [Target: 2.1]
```

## Swagger/OpenAPI

Gli endpoint disabilitati sono automaticamente rimossi dalla documentazione OpenAPI:

- `NotActiveOperationCustomizer`: Nasconde singole operazioni
- `NotActiveOpenApiCustomizer`: Rimuove path di controller disabilitati e pulisce tag vuoti

## Ordine di Precedenza

1. **Annotazione su metodo** ha precedenza su annotazione su classe
2. Se un metodo NON ha `@NotActive` ma la classe SÌ, il metodo è comunque disabilitato
3. Se un metodo HA `@NotActive` e la classe NO, solo quel metodo è disabilitato

## Casi d'Uso

### Sviluppo Feature

```java
@NotActive(reason = "In sviluppo - Sprint 15", targetVersion = "1.5.0")
@RestController
@RequestMapping("/api/v1/new-feature")
public class NewFeatureController {
    // Tutto il controller è nascosto fino al rilascio
}
```

### Deprecazione Graduale

```java
@RestController
@RequestMapping("/api/v1/legacy")
public class LegacyController {
    
    @NotActive(reason = "Usa /api/v2/users invece")
    @GetMapping("/users")
    public List<User> getUsers() { ... }
    
    // Altri endpoint ancora attivi...
}
```

### A/B Testing o Feature Flags

```java
// Può essere combinato con @ConditionalOnProperty per controllo runtime
@ConditionalOnProperty(name = "feature.experimental.enabled", havingValue = "true")
@RestController
@RequestMapping("/api/v1/experimental")
public class ExperimentalController {
    // Attivo solo se property è true
}
```

## Best Practices

1. **Sempre specificare `reason`** - Aiuta altri sviluppatori a capire perché è disabilitato
2. **Usare `targetVersion`** - Documenta quando l'endpoint sarà disponibile
3. **Rimuovere `@NotActive`** quando l'endpoint è pronto - Non lasciare codice morto
4. **Review periodica** - Controllare periodicamente gli endpoint disabilitati

## Note Tecniche

- L'interceptor esclude automaticamente path Swagger (`/swagger-ui/**`, `/v3/api-docs/**`)
- L'interceptor esclude path Actuator (`/actuator/**`)
- Il check avviene PRIMA dell'esecuzione del metodo (nel `preHandle`)
- Non interferisce con la sicurezza Spring Security (che viene eseguita prima)
