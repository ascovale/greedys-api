# Customer Matching System Documentation

## Overview

Il sistema di Customer Matching implementa un matching intelligente dei clienti basato su dati di contatto (telefono, email, nome) con configurazione personalizzabile per ristorante. Utilizza algoritmi di confidence scoring e fuzzy matching per identificare clienti esistenti durante il processo di prenotazione.

## Architecture Components

### Core Services

#### 1. PhoneNormalizer Service
- **Percorso**: `com.application.common.service.PhoneNormalizer`
- **Funzione**: Normalizzazione numeri di telefono in formato E.164
- **Caratteristiche**:
  - Supporto specifico per numeri italiani
  - Validazione formato E.164
  - Formattazione per display
  - Estrazione ultime N cifre per matching parziale

#### 2. FuzzyNameMatcher Service
- **Percorso**: `com.application.common.service.FuzzyNameMatcher`
- **Funzione**: Matching fuzzy dei nomi usando algoritmo Jaro-Winkler
- **Caratteristiche**:
  - Algoritmo Jaro-Winkler implementato internamente
  - Normalizzazione unicode (rimozione accenti)
  - Matching component-wise per nomi composti
  - Rimozione titoli comuni (Dott., Ing., etc.)

#### 3. CustomerMatchService Service
- **Percorso**: `com.application.customer.service.CustomerMatchService`
- **Funzione**: Engine principale di matching con logica di confidence scoring
- **Strategie di Matching**:
  1. **Phone Exact**: Confidence 1.0 - match esatto numero telefono
  2. **Email Exact**: Confidence 0.95 - match esatto email
  3. **Name Fuzzy**: Confidence 0.75-0.8 - similarity nomi usando Jaro-Winkler
- **Decisioni**:
  - `AUTO_ATTACH`: Confidence >= 0.95 (attach automatico)
  - `CONFIRM`: Confidence 0.75-0.94 (richiede conferma utente)
  - `CREATE_NEW`: Confidence < 0.75 (crea nuovo cliente)

#### 4. RestaurantSettingsService Service
- **Percorso**: `com.application.restaurant.service.RestaurantSettingsService`
- **Funzione**: Gestione configurazioni per ristorante (form schema, matching policy)
- **Caratteristiche**:
  - Schema JSON personalizzabile per form clienti
  - Policy di matching configurabile per ristorante
  - Configurazioni default fallback

### Data Models

#### RestaurantSettings Entity
- **Percorso**: `com.application.restaurant.persistence.model.RestaurantSettings`
- **Campi**:
  - `restaurantId`: UUID ristorante
  - `customerFormSchema`: JsonNode con configurazione form
  - `matchingPolicy`: JsonNode con policy di matching
  - `createdAt/updatedAt`: Timestamp gestione

#### DTO Matching System
- **CustomerMatchInput**: Dati input per matching (nome, telefono, email, restaurantId)
- **CustomerCandidateDTO**: Candidato match con confidence e reasoning
- **CustomerMatchDecisionDTO**: Decisione finale con tipo e motivazione
- **CustomerMatchResponse**: Risposta completa con candidati e decisione
- **CustomerFormSchemaDTO**: Schema configurabile form clienti

### API Endpoints

#### CustomerMatchController
- **Base Path**: `/api/customer/match`

##### Endpoints:
1. **POST /find**
   - **Funzione**: Trova matching clienti per input dati
   - **Input**: CustomerMatchInput
   - **Output**: CustomerMatchResponse con candidati e decisione

2. **GET /form-schema/{restaurantId}**
   - **Funzione**: Ottieni schema form clienti per ristorante
   - **Output**: CustomerFormSchemaDTO

3. **POST /form-schema/{restaurantId}**
   - **Funzione**: Aggiorna schema form clienti per ristorante
   - **Input**: CustomerFormSchemaDTO

4. **GET /by-phone**
   - **Funzione**: Ricerca rapida cliente per telefono
   - **Params**: phoneNumber, restaurantId
   - **Output**: CustomerMatchResponse

## Confidence Scoring Rules

### Thresholds
- **PHONE_EXACT**: 1.0 - Match esatto numero telefono
- **EMAIL_EXACT**: 0.95 - Match esatto email
- **NAME_PHONE_PARTIAL**: 0.85 - Match parziale telefono + nome simile
- **FULL_NAME_FUZZY**: 0.75 - Match fuzzy nome completo
- **NAME_SIMILARITY_THRESHOLD**: 0.8 - Soglia minima similarity nomi

### Decision Logic
```
if (confidence >= 1.0)           -> AUTO_ATTACH
if (confidence >= 0.95)          -> AUTO_ATTACH  
if (confidence >= 0.75)          -> CONFIRM
if (confidence >= 0.50)          -> CREATE_NEW (ma mostra candidati)
if (confidence < 0.50)           -> CREATE_NEW
```

## Form Schema Configuration

### CustomerFormSchemaDTO Fields
- **Required Fields**: requireFirstName, requireLastName, requirePhone, requireEmail
- **Optional Fields**: allowNickname, allowNotes  
- **Validation**: phoneFormat, emailValidation, phoneValidation
- **Labels**: Personalizzabili per lingua/brand
- **Placeholders**: Testi di aiuto personalizzabili

### Default Configuration
```json
{
  "requireFirstName": true,
  "requireLastName": true, 
  "requirePhone": true,
  "requireEmail": false,
  "phoneFormat": "italian",
  "emailValidation": true
}
```

## Usage Flow

### 1. Configurazione Iniziale
```java
// Configura schema form per ristorante
CustomerFormSchemaDTO schema = CustomerFormSchemaDTO.builder()
    .requirePhone(true)
    .requireEmail(false)
    .phoneFormat("italian")
    .build();
    
restaurantSettingsService.updateCustomerFormSchema(restaurantId, schema);
```

### 2. Matching Process
```java
// Input dati cliente da form
CustomerMatchInput input = CustomerMatchInput.builder()
    .phone("+393331234567")
    .firstName("Mario")
    .lastName("Rossi")
    .restaurantId(restaurantId)
    .build();

// Esegui matching
CustomerMatchResponse response = customerMatchService.findMatches(input);

// Gestisci risultato
switch (response.getDecision().getType()) {
    case AUTO_ATTACH:
        // Attach automatico al cliente esistente
        break;
    case CONFIRM:
        // Mostra candidati per conferma utente
        break;
    case CREATE_NEW:
        // Crea nuovo cliente
        break;
}
```

### 3. API Usage
```javascript
// Ottieni schema form
GET /api/customer/match/form-schema/{restaurantId}

// Esegui matching
POST /api/customer/match/find
{
  "phone": "+393331234567",
  "firstName": "Mario", 
  "lastName": "Rossi",
  "restaurantId": "uuid-restaurant"
}

// Ricerca rapida per telefono
GET /api/customer/match/by-phone?phoneNumber=3331234567&restaurantId=uuid
```

## Integration Points

### Customer Creation Flow
1. Frontend raccoglie dati usando schema configurato
2. Sistema esegue matching intelligente
3. Basandosi su decisione, frontend:
   - Attach automatico (alta confidence)
   - Mostra conferma candidati (media confidence)  
   - Procede con creazione nuovo (bassa confidence)

### Reservation System Integration
- Pre-matching durante inserimento dati prenotazione
- Prevenzione duplicati automatica
- Miglioramento user experience con auto-completion

## Performance Considerations

### Optimizations
- Matching per telefono/email usa indici database
- Matching per nome richiede scan completo (da ottimizzare)
- Cache possibile per schema ristoranti frequenti
- Limiting a 5 candidati massimi

### Scaling Options
- Indicizzazione text search per nomi
- Async matching per grandi datasets
- Configurazione soglie per ristorante
- Machine learning per confidence tuning

## Future Enhancements

### Planned Features
1. **ML-based Confidence**: Machine learning per tuning automatico soglie
2. **Address Matching**: Integrazione indirizzi nel matching
3. **Historical Behavior**: Utilizzo storico prenotazioni per confidence
4. **Bulk Import Matching**: Tool per import massivi con matching
5. **Analytics Dashboard**: Metrics efficacia matching per ristorante

### Configuration Extensions
1. **Custom Matching Rules**: Regole specifiche per settore/brand
2. **Multi-language Support**: Labels/placeholders localizzati
3. **Field Dependencies**: Logica condizionale tra campi form
4. **Validation Rules**: Regex personalizzati per validazioni

## Error Handling

### Common Scenarios
- **Phone Normalization Failure**: Fallback a stringa originale
- **Database Errors**: Graceful degradation con logging
- **Invalid Schema**: Utilizzo schema default
- **Timeout Matching**: Risposta rapida con confidence bassa

### Logging Strategy
- **DEBUG**: Dettagli algoritmi matching
- **INFO**: Decisioni matching e performance
- **ERROR**: Fallimenti sistema con stack trace
- **AUDIT**: Configurazioni schema ristorante