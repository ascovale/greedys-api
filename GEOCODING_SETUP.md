# Guida alla Configurazione delle API per Greedys

## 🗺️ Configurazione Google Maps API

### 1. Ottenere la Google Maps API Key

1. **Vai alla Google Cloud Console**: https://console.cloud.google.com/
2. **Crea un nuovo progetto** o seleziona un progetto esistente
3. **Abilita le API necessarie**:
   - Vai su "API e Servizi" > "Libreria"
   - Cerca e abilita "Geocoding API"
   - Opzionalmente abilita "Google My Business API" se intendi usare le funzionalità di verifica ristorante

4. **Crea una API Key**:
   - Vai su "API e Servizi" > "Credenziali"
   - Clicca "Crea credenziali" > "Chiave API"
   - Copia la chiave generata

5. **Configura le restrizioni della API Key** (IMPORTANTE per la sicurezza):
   - Clicca sulla chiave appena creata
   - In "Restrizioni applicazione", seleziona "Server IP" e aggiungi il tuo IP del server
   - In "Restrizioni API", seleziona "Limita chiave" e scegli solo le API che usi (Geocoding API)

### 2. Configurazione delle Variabili d'Ambiente

#### Opzione A: Variabili d'Ambiente del Sistema (Raccomandato per Produzione)

**Windows (PowerShell):**
```powershell
[Environment]::SetEnvironmentVariable("GOOGLE_MAPS_API_KEY", "la_tua_api_key_qui", "User")
```

**Linux/Mac:**
```bash
export GOOGLE_MAPS_API_KEY="la_tua_api_key_qui"
```

#### Opzione B: File .env (Per Sviluppo Locale)

1. Copia il file `.env.example` in `.env`:
```bash
cp .env.example .env
```

2. Modifica il file `.env` e inserisci la tua API key:
```
GOOGLE_MAPS_API_KEY=la_tua_api_key_qui
```

#### Opzione C: Docker Secrets (Per Produzione Docker)

1. Crea il file secret:
```bash
echo "la_tua_api_key_qui" | docker secret create google_maps_api_key -
```

2. Aggiorna il `docker-compose.yml` per usare il secret

### 3. Configurazione Google OAuth (Opzionale)

Se vuoi usare l'autenticazione Google o la verifica dei ristoranti con Google My Business:

1. **Nella Google Cloud Console**:
   - Vai su "API e Servizi" > "Credenziali"
   - Clicca "Crea credenziali" > "ID client OAuth 2.0"
   - Seleziona "Applicazione web"
   - Aggiungi gli URI di reindirizzamento autorizzati

2. **Configura le variabili**:
```
GOOGLE_OAUTH_CLIENT_ID=il_tuo_client_id
GOOGLE_OAUTH_CLIENT_SECRET=il_tuo_client_secret
```

## 🧪 Test della Configurazione

### 1. Verifica che il Servizio si Avvii

Dopo aver configurato la API key, avvia l'applicazione:

```bash
./mvnw spring-boot:run
```

Controlla i log per eventuali errori di configurazione.

### 2. Test del Geocoding

Puoi testare il geocoding usando l'endpoint REST o direttamente nel codice:

**Esempio di test manuale:**
```java
@Autowired
private GeocodingService geocodingService;

// Test con Google Maps API
GeocodingDTO result = geocodingService.geocodeAddress("Via Roma 1, Milano");

// Test fallback con Nominatim (se Google non è disponibile)
GeocodingDTO result2 = geocodingService.geocodeAddress("Piazza del Duomo, Firenze");
```

### 3. Monitoring e Quotas

1. **Monitora l'uso della API**:
   - Vai su Google Cloud Console > "API e Servizi" > "Quotas"
   - Controlla l'utilizzo della Geocoding API

2. **Imposta avvisi di quota**:
   - Configura avvisi quando ti avvicini ai limiti di quota
   - La quota gratuita di Google Maps include 40.000 richieste/mese

## ⚠️ Gestione Errori e Fallback

Il servizio è configurato per:

1. **Provare prima Google Maps API** (se la key è configurata)
2. **Usare Nominatim come fallback** (gratuito, senza API key)
3. **Gestire gracefully gli errori** senza crashare l'applicazione

## 🔒 Sicurezza

- **Non committare mai** la API key nel repository
- **Usa sempre restrizioni IP** sulla API key
- **Monitora l'uso** per rilevare abusi
- **Ruota periodicamente** le API key

## 📝 Log e Debugging

Per abilitare logging dettagliato del geocoding, aggiungi in `application.properties`:

```properties
logging.level.com.application.restaurant.service.GeocodingService=DEBUG
logging.level.org.springframework.web.client.RestTemplate=DEBUG
```

## 🚀 Produzione

Per la produzione:

1. **Usa Docker Secrets** o un sistema di gestione secret sicuro
2. **Configura un reverse proxy** (nginx) con rate limiting
3. **Monitora le metriche** di utilizzo delle API
4. **Imposta backup e failover** per i servizi critici

## 📞 Supporto

Se incontri problemi:
1. Controlla i log dell'applicazione
2. Verifica che la API key sia valida e abbia le giuste permissions
3. Testa la connettività di rete
4. Verifica le quote Google Cloud Console
