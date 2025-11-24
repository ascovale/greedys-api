# WebSocket 400 Bad Request - Backend Debug Guide

**Data**: 24 Novembre 2025  
**Problema**: Client WebSocket riceve HTTP 400 durante handshake  
**Status**: 🔴 **CRITICO - Blocca le notifiche real-time**

---

## ANALISI DELLA SEGNALAZIONE CLIENT

Il client app (Flutter Web) riporta:
- ✅ Login funziona → JWT valido
- ✅ JWT appena emesso (iat/exp coerenti)
- ✅ Tentativo connessione a `wss://api.greedys.it/ws`
- ✅ Query params inviati: `?token=JWT&access_token=JWT`
- ❌ **Risposta backend: HTTP 400 Bad Request**
- ❌ **Connessione mai stabilita**

---

## COSA CONTROLLARE SUL BACKEND

### 1️⃣ ENDPOINT REGISTRATION (CRITICO)

**File**: `src/main/java/com/application/config/WebSocketConfig.java`

Verificare che il STOMP endpoint sia registrato:

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")  // ✅ Deve permettere all origins
                .withSockJS();           // ✅ SockJS fallback
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
}
```

**Domande**:
- [ ] Il file esiste?
- [ ] Path `/ws` è esattamente quello? (non `/socket`, `/ws/connect`, ecc)
- [ ] `setAllowedOrigins("*")` è impostato?
- [ ] `withSockJS()` è abilitato?

---

### 2️⃣ JWT TOKEN VALIDATION (CRITICO)

**File**: `src/main/java/com/application/config/WebSocketSecurityConfig.java` o simile

Verificare che il WebSocket handshake validi il JWT dai query parameters:

```java
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) 
            throws Exception {
        
        // 🔍 ESTRAI TOKEN DAI QUERY PARAMETERS
        String query = request.getURI().getQuery();
        log.debug("WebSocket handshake query: {}", query);
        
        // Token può essere in ?token=JWT o ?access_token=JWT
        String token = extractToken(query, "token");
        if (token == null) {
            token = extractToken(query, "access_token");
        }
        
        if (token == null || token.isEmpty()) {
            log.warn("❌ No JWT token found in query parameters");
            return false;  // ❌ Questo causerebbe 400!
        }
        
        try {
            // ✅ VALIDA IL TOKEN
            if (!jwtUtil.isAccessToken(token)) {
                log.warn("❌ Token is not an access token");
                return false;
            }
            
            String username = jwtUtil.extractUsername(token);
            log.info("✅ WebSocket JWT valid for user: {}", username);
            
            attributes.put("username", username);
            attributes.put("jwt", token);
            return true;  // ✅ Handshake OK
            
        } catch (Exception e) {
            log.error("❌ JWT validation failed: {}", e.getMessage());
            return false;  // ❌ Questo causerebbe 400!
        }
    }

    private String extractToken(String query, String paramName) {
        if (query == null) return null;
        String prefix = paramName + "=";
        int start = query.indexOf(prefix);
        if (start == -1) return null;
        int end = query.indexOf("&", start);
        if (end == -1) {
            return query.substring(start + prefix.length());
        }
        return query.substring(start + prefix.length(), end);
    }
}
```

**Domande**:
- [ ] Esiste un `HandshakeInterceptor`?
- [ ] Se esiste, sta validando il JWT dai query parameters?
- [ ] Se esiste, è registrato in `WebSocketConfig`?
- [ ] Il metodo `beforeHandshake` torna `true` per token validi?

**Se il file NON esiste**: 👉 **QUESTO È IL PROBLEMA** - WebSocket non sa come validare il JWT!

---

### 3️⃣ REGISTRAZIONE DELL'INTERCEPTOR

**File**: `WebSocketConfig.java` (stesso file di 1️⃣)

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(handshakeInterceptor)  // 👈 DEVE ESSERCI!
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

**Domanda**:
- [ ] L'interceptor è registrato con `.addInterceptors(handshakeInterceptor)`?

---

### 4️⃣ SECURITY CONFIGURATION

**File**: `src/main/java/com/application/config/SecurityConfig.java`

Verificare che WebSocket sia escluso dalla security chain:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/ws", "/ws/**").permitAll()  // ✅ WebSocket free
                .requestMatchers("/customer/**", "/restaurant/**").permitAll()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf.disable())
            .build();
        
        return http.build();
    }
}
```

**Domanda**:
- [ ] `/ws` e `/ws/**` sono permessi senza authentication?

---

### 5️⃣ LOG DEL BACKEND

Per capire **ESATTAMENTE** perché ritorna 400, attivare DEBUG logging:

**File**: `src/main/resources/application.properties` o `application-dev.yml`

```properties
# WebSocket Debug Logging
logging.level.com.application.config.WebSocketConfig=DEBUG
logging.level.com.application.config.WebSocketHandshakeInterceptor=DEBUG
logging.level.org.springframework.web.socket=DEBUG
logging.level.org.springframework.messaging.simp=DEBUG
logging.level.org.springframework.security=DEBUG
```

Poi:
1. Prendi il JWT dal client log
2. Fai tentativo di connessione WebSocket
3. Guarda i backend logs per il messaggio di errore specifico
4. Scrivi qui il messaggio di errore

**Esempio di output atteso**:
```
DEBUG com.application.config.WebSocketHandshakeInterceptor
WebSocket handshake query: token=eyJhbGciOiJIUzI1NiJ9...&access_token=eyJhbGc...

DEBUG com.application.util.JwtUtil
JWT validation: username=restaurant@example.com, expires=1764020546

INFO com.application.config.WebSocketHandshakeInterceptor
✅ WebSocket JWT valid for user: restaurant@example.com
```

---

## POSSIBILI CAUSE RANKEATE

### 🔴 CRITICA (95% probabilità)

**CAUSA**: File `WebSocketHandshakeInterceptor` **NON ESISTE**
- **Sintomo**: Handshake intercettato da Spring di default, senza validazione JWT
- **Risultato**: 400 Bad Request automatico
- **Soluzione**: Creare il file di cui sopra

### 🔴 CRITICA (80% probabilità)

**CAUSA**: Endpoint `/ws` NON registrato
- **Sintomo**: 404 Not Found (o proxy lo redirige a 400)
- **Risultato**: Client riceve 400
- **Soluzione**: Verificare `WebSocketConfig.registerStompEndpoints()`

### 🟡 ALTA (60% probabilità)

**CAUSA**: JWT token **SCADUTO**
- **Sintomo**: Token ha `exp` nel passato
- **Risultato**: Validazione fallisce → 400
- **Soluzione**: Client deve fare nuovo login per token fresco

### 🟡 MEDIA (40% probabilità)

**CAUSA**: JWT Secret **MISMATCH**
- **Sintomo**: Token generato con secret A, validato con secret B
- **Risultato**: Firma JWT non valida → 400
- **Soluzione**: Verificare `JwtUtil` usa stesso secret di `AuthenticationService`

### 🟡 MEDIA (30% probabilità)

**CAUSA**: Traefik proxy non forwarda WebSocket correttamente
- **Sintomo**: WebSocket upgrade headers persi in transito
- **Risultato**: Backend riceve richiesta malformata → 400
- **Soluzione**: Verificare config Traefik per WebSocket

---

## AZIONI IMMEDIATE

### ✅ Step 1: Verifica File Existence (5 min)

```bash
# Cerca WebSocketHandshakeInterceptor
find /home/valentino/workspace/greedysgroup/greedys_api -name "*HandshakeInterceptor*" -o -name "*WebSocketInterceptor*"

# Cerca WebSocketConfig
find /home/valentino/workspace/greedysgroup/greedys_api -name "WebSocketConfig*"

# Cerca Security WebSocket config
grep -r "registerStompEndpoints" /home/valentino/workspace/greedysgroup/greedys_api/greedys_api/src
```

### ✅ Step 2: Attiva DEBUG Logging (2 min)

Aggiungi a `application.properties`:
```properties
logging.level.org.springframework.web.socket=DEBUG
logging.level.com.application.config=DEBUG
```

### ✅ Step 3: Riproduci Errore (1 min)

1. Fai login dal client
2. Copia JWT dal client log
3. Prova connessione WebSocket
4. Cattura backend logs

### ✅ Step 4: Condividi Output

Qui nel chat:
```
1. Output del Step 1 (file search)
2. Contenuto di WebSocketConfig.java (se esiste)
3. Contenuto di WebSocketHandshakeInterceptor.java (se esiste)
4. Backend logs dal Step 3 con JWT validation error
```

---

## CHECKLIST FINALE

- [ ] WebSocketConfig esiste? 
- [ ] Endpoint `/ws` è registrato?
- [ ] WebSocketHandshakeInterceptor esiste?
- [ ] L'interceptor valida JWT da query params?
- [ ] L'interceptor è registrato in WebSocketConfig?
- [ ] `/ws` è permesso da SecurityConfig?
- [ ] DEBUG logging attivato su application.properties?
- [ ] Tentativo di connessione fatto?
- [ ] Backend logs catturati?
- [ ] Messaggio di errore specifico identificato?

---

## SUPPORTO

Se hai completato la checklist e il problema persiste, condividi:

1. **WebSocketConfig.java** - Contenuto completo
2. **WebSocketHandshakeInterceptor.java** - Contenuto completo (o "file non esiste")
3. **Backend logs** - Ultimi 50 righe di logs con JWT validation error
4. **JWT Token** - I primi 100 caratteri del token dal client

Questo vi permette di identificare il problema in 5 minuti.
