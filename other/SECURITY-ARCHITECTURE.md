# Security Architecture

Questo documento descrive la configurazione di sicurezza dell'applicazione: catene di filtri (filter chain), filtri custom, pattern pubblici, gestione delle risorse statiche e come estendere o diagnosticare problemi.

## Panoramica

L'app usa Spring Security con più SecurityFilterChain specifiche per dominio e una chain di default:
- /restaurant/**
- /customer/**
- /admin/**
- default (per tutto il resto, inclusi statici e documentazione)

I filtri custom (JWT e validazione tipo token) sono inseriti solo nelle chain appropriate e non sono registrati globalmente.

## Catene di filtri (SecurityFilterChain)

File: `com.application.common.spring.SecurityConfig`

- Common
  - CSRF disabilitato (stateless)
  - CORS abilitato (origini/metodi/header aperti, esposti Authorization e Content-Type)
  - SessionCreationPolicy.STATELESS
  - PasswordEncoder: BCryptPasswordEncoder
  - AuthenticationManager di default annotato @Primary che non deve essere usato (solleva eccezione se invocato)
  - HttpSessionEventPublisher

### /restaurant/**
Ordine filtri:
1) TokenTypeValidationFilter (before UsernamePasswordAuthenticationFilter)
2) RUserHubValidationFilter (after TokenTypeValidationFilter)
3) RUserRequestFilter (after RUserHubValidationFilter)

Autorizzazioni:
- permitAll sui pattern pubblici di dominio: `SecurityPatterns.getRestaurantPublicPatterns()`
- authenticated su tutte le altre richieste `/restaurant/**`

### /customer/**
Ordine filtri:
1) TokenTypeValidationFilter (before UsernamePasswordAuthenticationFilter)
2) CustomerRequestFilter (after TokenTypeValidationFilter)

Autorizzazioni:
- permitAll su `SecurityPatterns.getCustomerPublicPatterns()`
- authenticated su altre `/customer/**`

### /admin/**
Ordine filtri:
1) TokenTypeValidationFilter (before UsernamePasswordAuthenticationFilter)
2) AdminRequestFilter (after TokenTypeValidationFilter)

Autorizzazioni:
- permitAll su `SecurityPatterns.getAdminPublicPatterns()`
- authenticated su altre `/admin/**`

### Chain di default
- permitAll su `SecurityPatterns.DEFAULT_PUBLIC_PATTERNS`
- anyRequest().permitAll() per evitare di bloccare endpoint generici; i check reali avvengono nelle chain specifiche

## Registrazione dei filtri: solo nelle chain, non globalmente

Per evitare che i filtri intercettino tutte le richieste (inclusi statici) o vengano eseguiti in ordine non controllato, la loro registrazione globale è disabilitata.

File: `SecurityConfig`
- Vengono esposti `FilterRegistrationBean<...>` per ciascun filtro con `setEnabled(false)`
- I filtri sono poi aggiunti manualmente alle rispettive SecurityFilterChain via `.addFilter...`

Vantaggi:
- Evita doppie esecuzioni dello stesso filtro
- Evita che i filtri tocchino risorse statiche o endpoint non pertinenti
- Ordine dei filtri sotto controllo per catena

## Pattern pubblici e risorse statiche

Esistono due sorgenti complementari di pattern pubblici:

1) `SecurityPatterns` (classe utility)
   - `GLOBAL_PUBLIC_PATTERNS`: include root `/`, `/index.html`, swagger, actuator, e statici: `/favicon.ico`, `/css/**`, `/js/**`, `/img/**`, `/images/**`, `/static/**`
   - Pattern per dominio (auth/login/refresh/open) combinati nella chain: `getRestaurantPublicPatterns()`, `getCustomerPublicPatterns()`, `getAdminPublicPatterns()`
   - `DEFAULT_PUBLIC_PATTERNS` per la chain di default

2) `SecurityEndpointConfig` (properties centralizzate)
   - `publicPaths`: include `/`, `/index.html`, `/img/**`, swagger e auth per i tre domini
   - Usato dai filtri per decidere rapidamente se bypassare una richiesta

Le risorse statiche sono servite da Spring Boot dal classpath `src/main/resources/static`. Con i pattern sopra, sono accessibili senza autenticazione.

## Filtri custom: responsabilità e bypass

### TokenTypeValidationFilter
File: `com.application.common.security.TokenTypeValidationFilter`

Responsabilità:
- Controlla che l'header Authorization sia presente per endpoint protetti
- Distingue Access Token vs Refresh Token
- Vincola i refresh agli endpoint di refresh (incluso caso speciale Hub)
- Controlla compatibilità `userType` del token con il prefisso dell’endpoint
- Produce errori JSON coerenti con `ResponseWrapper`

Bypass (shouldNotFilter):
- Salta `OPTIONS` (preflight CORS)
- Salta path pubblici: unione tra `SecurityPatterns.isPublicPath()` e `SecurityEndpointConfig.isPublicPath()`
- Salta esplicitamente statici comuni: `/`, `/index.html`, `/favicon.ico`, `/img/**`, `/css/**`, `/js/**`, `/images/**`, `/static/**`

Nota: il filtro non è globale; gira solo nelle chain dove viene aggiunto.

### RUserHubValidationFilter
- Se il token è di tipo “Hub”, limita l’accesso ai soli endpoint Hub-allowed (es. `switch-restaurant`, `available-restaurants`, `logout`, `profile/hub`, `auth/refresh`)
- I pattern sono definiti in `SecurityEndpointConfig` (set `hubAllowedPaths`)

### RUserRequestFilter / CustomerRequestFilter / AdminRequestFilter
- Eseguono l’autenticazione JWT per il rispettivo dominio popolando il SecurityContext
- Bypassano i path pubblici via `SecurityPatterns.isPublicPath()`
- Rifiutano l’uso di refresh token su endpoint protetti

## Gestione CORS

File: `SecurityConfig.corsConfigurationSource()`
- `AllowCredentials = true`
- `AllowedOriginPatterns = *`
- `AllowedHeaders = *`
- `AllowedMethods = [GET, POST, PUT, DELETE, OPTIONS, PATCH]`
- `ExposedHeaders = [Authorization, Content-Type]`

## Provider di autenticazione

- `RUserAuthenticationProvider`, `CustomerAuthenticationProvider`, `AdminAuthenticationProvider`
  - Iniettati nelle rispettive chain via `.authenticationProvider(...)`
  - Separati dagli AuthenticationManager usati per login

## Flussi tipici

1) GET /index.html
- default chain → permitAll
- Filtri custom: non eseguiti (non registrazione globale + bypass statici)
- 200 OK statico

2) POST /customer/auth/login
- chain `/customer/**` → permitAll (pattern auth)
- TokenTypeValidationFilter: bypass per path pubblico
- CustomerRequestFilter: bypass per path pubblico
- Controller gestisce il login e rilascia token

3) GET /customer/orders con Authorization: Bearer <access>
- chain `/customer/**` → authenticated
- TokenTypeValidationFilter: verifica access token
- CustomerRequestFilter: autentica l’utente (SecurityContext)
- Controller esegue con utente autenticato

4) POST /restaurant/user/auth/refresh con refresh token
- chain `/restaurant/**` → permitAll per endpoint di refresh
- TokenTypeValidationFilter: consente solo refresh token (o hub refresh per endpoint dedicato)
- Nessuna autenticazione via RUserRequestFilter

## Come estendere

- Nuovo endpoint pubblico statico: aggiungere il path a `SecurityPatterns.GLOBAL_PUBLIC_PATTERNS` e, se serve, a `SecurityEndpointConfig.publicPaths`
- Nuovo endpoint pubblico di dominio: aggiungere ai pattern di dominio in `SecurityPatterns` (e opzionalmente in `SecurityEndpointConfig`)
- Nuova chain: definire un nuovo `.securityMatcher("/prefix/**")`, regole authorize e inserire i filtri nell’ordine desiderato
- Nuovo filtro: disabilitare la registrazione globale con `FilterRegistrationBean#setEnabled(false)` e aggiungerlo nelle chain necessarie

## Troubleshooting

- 401 su risorse statiche (es. /index.html):
  - Verifica che il filtro non sia registrato globalmente (controlla `FilterRegistrationBean` disabilitati)
  - Verifica che `/` e `/index.html` siano nei pattern pubblici
  - Controlla il `shouldNotFilter` del TokenTypeValidationFilter

- 403 con token Hub su endpoint non permessi:
  - Controlla i pattern `hubAllowedPaths` in `SecurityEndpointConfig`

- Refresh non funzionante:
  - Controlla gli endpoint hardcoded di refresh in `TokenTypeValidationFilter.isRefreshEndpoint`
  - Usa il token corretto (refresh vs access vs hub refresh)

## Riferimenti file

- `com.application.common.spring.SecurityConfig`
- `com.application.common.security.SecurityPatterns`
- `com.application.common.security.config.SecurityEndpointConfig`
- `com.application.common.security.TokenTypeValidationFilter`
- `com.application.restaurant.RUserRequestFilter`
- `com.application.customer.CustomerRequestFilter`
- `com.application.admin.AdminRequestFilter`
- `src/main/resources/static/` (risorse statiche: index.html, img, css, js)
