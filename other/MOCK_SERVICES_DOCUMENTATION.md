# Mock Services per Sviluppo Minimal

Questo documento descrive il sistema di mock services implementato per la modalità di sviluppo minimal di Greedys API.

## Panoramica

Il sistema di mock services permette di disabilitare le dipendenze esterne costose durante lo sviluppo, sostituendole con implementazioni simulate che:

- ✅ Consentono startup ultra-veloce (30-60 secondi invece di 5-10 minuti)
- ✅ Non richiedono connessioni a servizi esterni
- ✅ Forniscono risposte deterministiche per il testing
- ✅ Mantengono la compatibilità API completa
- ✅ Utilizzano logging dettagliato per debugging

## Mock Services Implementati

### 1. MockFirebaseService
**File**: `MockFirebaseService.java`
**Sostituisce**: `FirebaseService`
**Attivazione**: `firebase.enabled=false`

**Funzionalità Mock**:
- `verifyToken()`: Simula validazione token Firebase
- `sendNotification()`: Logga dettagli notifica senza invio reale

### 2. MockGoogleAuthService
**File**: `MockGoogleAuthService.java`
**Sostituisce**: `GoogleAuthService`
**Attivazione**: `google.oauth.enabled=false`

**Funzionalità Mock**:
- `authenticateWithGoogle()`: Genera email utente mock da token
- Gestisce creazione/login utenti senza chiamate Google reali
- Mantiene flusso completo di autenticazione con JWT

### 3. MockGooglePlacesSearchService
**File**: `MockGooglePlacesSearchService.java`
**Sostituisce**: `GooglePlacesSearchService`
**Attivazione**: `google.maps.enabled=false`

**Funzionalità Mock**:
- `findRestaurantsOnMaps()`: Genera PlaceDetails mock con dati realistici
- `getPlaceDetailsByPlaceId()`: Crea dettagli luoghi simulati
- `findRestaurantsOnMapFromRestaurant()`: Simula ricerca da entità Restaurant
- Coordinate geografiche centrate su Roma con variazioni casuali

### 4. MockTwilioConfig
**File**: `MockTwilioConfig.java` (già esistente, completato)
**Sostituisce**: `TwilioConfig`
**Attivazione**: `twilio.enabled=false`

**Funzionalità Mock**:
- Fornisce configurazioni Twilio simulate
- Evita inizializzazione Twilio SDK reale

### 5. MockReliableNotificationService
**File**: `MockReliableNotificationService.java`
**Sostituisce**: `ReliableNotificationService`
**Attivazione**: `notifications.enabled=false`

**Funzionalità Mock**:
- `sendEmailWithRetry()`: Simula invio email con retry
- Logga dettagli senza invio email reale

## Configurazione

### File di Configurazione
Il file `application-dev-minimal.properties` contiene:

```properties
# Abilitazione Mock Services
firebase.enabled=false
google.oauth.enabled=false
google.maps.enabled=false
twilio.enabled=false
notifications.enabled=false

# Configurazioni Mock (valori fittizi)
google.oauth.web.client.id=mock-web-client-id
google.oauth.flutter.client.id=mock-flutter-client-id
google.oauth.android.client.id=mock-android-client-id
google.oauth.ios.client.id=mock-ios-client-id
google.maps.api.key=mock-google-maps-api-key
twilio.account.sid=mock-account-sid
twilio.auth.token=mock-auth-token
# ... altre configurazioni mock
```

### Attivazione
I mock services si attivano automaticamente quando:
1. **Profilo Maven**: `minimal` è attivo
2. **Proprietà**: Le relative `*.enabled=false` sono impostate
3. **Annotazione**: `@ConditionalOnProperty` valuta le condizioni

## Utilizzo negli Script

### Script Esistenti
```bash
# Sviluppo standard (tutti i servizi)
./dev.sh

# Sviluppo veloce (mock services)
./dev-fast.sh
```

### Comandi Maven Diretti
```bash
# Compilazione con mock
mvn clean compile -Pminimal

# Avvio con mock
mvn spring-boot:run -Pminimal -Dspring.profiles.active=dev-minimal
```

## Vantaggi per il Developing

### Performance
- **Startup**: 30-60 secondi (vs 5-10 minuti)
- **Memory**: Ridotto uso memoria (no SDK esterni)
- **Network**: Zero dipendenze di rete

### Debugging
- **Logging**: Dettagliato con emoji per visibilità
- **Determinismo**: Risposte prevedibili per testing
- **Isolamento**: Nessun side-effect su servizi reali

### Testing
- **Unità**: Test isolati senza mock esterni
- **Integrazione**: Flussi completi senza costi servizi
- **CI/CD**: Build rapidi senza credenziali esterne

## Pattern Implementativo

### Struttura Mock Service
```java
@Service
@Primary  // Sovrascrive implementazione originale
@ConditionalOnProperty(name = "service.enabled", havingValue = "false")
public class MockServiceImpl extends OriginalService {
    
    public MockServiceImpl() {
        // Bypass costruttore originale se necessario
        log.warn("🔧 MOCK: ServiceName attivato - modalità sviluppo minimal");
    }
    
    @Override
    public ReturnType methodName(Parameters params) {
        log.info("🔧 MOCK: Operation chiamata con: {}", params);
        // Logica mock deterministica
        return mockResult;
    }
}
```

### Logging Pattern
- **Prefisso**: `🔧 MOCK:` per identificazione visiva
- **Dettagli**: Parametri input/output loggati
- **Emoji**: 📧📱🎯 per categorizzazione rapida

## Compatibilità

### Produzione
- **Zero Impact**: Mock disabilitati automaticamente in produzione
- **Docker**: Configurazione Docker invariata
- **Secrets**: Sistema secrets esistente mantenuto

### Development
- **Hot Swap**: Possibile switch tra mock/reale via properties
- **Debugging**: Mock e servizi reali coesistono
- **Testing**: Test suite funziona con entrambe le modalità

## Troubleshooting

### Mock Non Attivo
1. Verificare `*.enabled=false` in properties
2. Controllare profilo Maven attivo
3. Verificare logs per "🔧 MOCK: ServiceName attivato"

### Errori Compilazione
1. Verificare import corretti nei mock
2. Controllare override method signatures
3. Validare `@ConditionalOnProperty` syntax

### Comportamento Inaspettato
1. Controllare logs mock per conferma attivazione
2. Verificare dati mock generati
3. Confrontare con implementazione originale

## Estensioni Future

### Nuovi Mock Services
Per aggiungere nuovi mock:
1. Creare `Mock{ServiceName}.java`
2. Estendere/implementare servizio originale
3. Aggiungere `@ConditionalOnProperty` appropriato
4. Aggiungere proprietà `{service}.enabled=false`
5. Documentare comportamento mock

### Configurazioni Avanzate
- Mock data personalizzati
- Simulazioni failure scenarios
- Latency simulation per performance testing
- Mock response recording/playback

---

**Autore**: Sistema Mock Services Greedys API  
**Versione**: 1.0  
**Data**: 2024
