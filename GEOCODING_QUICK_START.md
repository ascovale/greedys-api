# 🗺️ Configurazione Geocoding - Guida Rapida

Il tuo servizio di geocoding è ora configurato e pronto all'uso! Segui questi passaggi per completare la configurazione.

## ⚡ Configurazione Rapida

### 1. Ottieni la Google Maps API Key

1. Vai su [Google Cloud Console](https://console.cloud.google.com/)
2. Crea/seleziona un progetto
3. Abilita "Geocoding API"
4. Crea una API Key
5. Configura le restrizioni di sicurezza

### 2. Configura la API Key

#### 🔧 Configurazione Manuale:

**Opzione A - Variabile d'ambiente (Windows):**
```powershell
[Environment]::SetEnvironmentVariable("GOOGLE_MAPS_API_KEY", "la_tua_api_key_qui", "User")
```

**Opzione B - Aggiungi direttamente in application.properties:**
```properties
# Sostituisci questa riga nel file application.properties:
geocoding.google.apiKey=la_tua_api_key_qui
```

### 3. Avvia l'Applicazione

```powershell
# Avvia l'app e controlla i logs
./mvnw spring-boot:run

# Oppure con Docker
docker-compose up
```

## 🎯 Caratteristiche Principali

### ✅ Doppio Fallback
- **Primario**: Google Maps API (accurato, richiede API key)
- **Fallback**: OpenStreetMap Nominatim (gratuito, senza API key)

### ✅ Arricchimento Automatico
- Coordinate GPS
- Città, provincia, stato
- Codice postale
- Paese
- Indirizzo formattato

### ✅ Validazione Intelligente
- Confronto codici postali
- Verifica coerenza città
- Gestione errori graceful

## 📁 File di Configurazione

- `GEOCODING_SETUP.md` - Guida dettagliata completa
- `.env.example` - Template per variabili d'ambiente
- `GeocodingService.java` - Servizio geocoding configurato
- `GeocodingConfig.java` - Classe di configurazione Spring

## 🚀 Utilizzo nel Codice

```java
@Autowired
private GeocodingService geocodingService;

// Geocoding semplice
GeocodingDTO result = geocodingService.geocodeAddress("Via Roma 1, Milano");

// Con contesto città per maggiore accuratezza
GeocodingDTO result = geocodingService.geocodeAddress("Via Roma 1", "Milano");

// Arricchimento automatico ristorante
geocodingService.enrichRestaurantWithGeocodingData(restaurant);
```

## 🔒 Sicurezza

- ✅ Variabili d'ambiente per API Key
- ✅ Restrizioni IP configurabili su Google Cloud
- ✅ Fallback automatico senza API key
- ✅ Rate limiting Google Cloud Console

## 📞 Troubleshooting

| Problema | Soluzione |
|----------|-----------|
| API Key non funziona | Verifica restrizioni in Google Cloud Console |
| Quota esaurita | Controlla usage in Google Cloud Console |
| Errori di rete | Usa Nominatim fallback (automatico) |
| Risultati imprecisi | Aggiungi contesto città |

## 💰 Costi

- **Google Maps**: 40.000 richieste/mese gratuite
- **Nominatim**: Completamente gratuito (con rate limit)

---

🎉 **La tua configurazione è completa!** 

Il servizio funzionerà anche senza Google Maps API key usando Nominatim come fallback.
