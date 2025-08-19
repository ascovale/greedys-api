# Supporto Globale per Partite IVA / Numeri di Identificazione Fiscale

## Panoramica

Il validator `@ValidVatNumber` supporta ora numeri di identificazione fiscale da **65+ paesi** in tutto il mondo, non solo l'Unione Europea.

## Copertura per Regione

### 🇪🇺 **Unione Europea (27 paesi)**
Standard VIES - Tutti i paesi UE supportati:
- **Italia**: `IT12345678901`
- **Francia**: `FR12345678901` 
- **Germania**: `DE123456789`
- **Spagna**: `ESA12345674`
- **E tutti gli altri 23 paesi UE**

### 🌍 **Europa (non-UE)**
- **Regno Unito**: `GB123456789`
- **Svizzera**: `CHE123456789MWST`
- **Norvegia**: `NO123456789MVA`
- **Russia**: `RU1234567890`
- **E altri paesi europei**

### 🇺🇸 **Nord America**
- **Stati Uniti**: `US12-3456789` (EIN)
- **Canada**: `CA123456789RT0001` (GST/HST)
- **Messico**: `MXABC123456DEF` (RFC)

### 🌏 **Asia-Pacifico**
- **Australia**: `AU12345678901` (ABN)
- **Giappone**: `JP1234567890123`
- **Singapore**: `SG12345678A` (UEN)
- **India**: `IN22AAAAA0000A1Z5` (GSTIN)
- **Hong Kong**: `HK12345678`
- **E molti altri**

### 🇧🇷 **Sud America**
- **Brasile**: `BR12.345.678/0001-90` (CNPJ)
- **Argentina**: `AR12345678901` (CUIT)
- **Cile**: `CL12345678-9` (RUT)
- **Colombia**: `CO123456789` (NIT)
- **E altri paesi sudamericani**

### 🌍 **Africa**
- **Sudafrica**: `ZA1234567890`
- **Egitto**: `EG123456789`
- **Marocco**: `MA12345678`
- **Tunisia**: `TN1234567ABC123`

### 🏛️ **Medio Oriente**
- **Emirati Arabi Uniti**: `AE123456789012345` (TRN)
- **Arabia Saudita**: `SA123456789012345`
- **Israele**: `IL123456789`
- **Turchia**: `TR1234567890`

## Caratteristiche del Validator

### ✅ **Formati Supportati**
- **Spazi e trattini**: `IT 1234 5678 901` o `IT-1234-5678-901`
- **Maiuscole/minuscole**: `it12345678901` → convertito in `IT12345678901`
- **Formati nazionali specifici**: Ogni paese ha le sue regole

### 🔧 **Configurazione Flessibile**
```java
@ValidVatNumber // Permette valori null
@ValidVatNumber(allowNull = false) // Richiede valore obbligatorio
@ValidVatNumber(message = "Messaggio personalizzato") // Messaggio custom
```

### 🌐 **Messaggi di Errore Localizzati**
- **Italiano**: "Numero di partita IVA non valido per Italia. Formato atteso: IT + 11 cifre"
- **Inglese**: "Invalid VAT number for Italy. Expected format: IT + 11 digits"
- **Spagnolo**: "Número de IVA inválido para Italia. Formato esperado: IT + 11 dígitos"

## Utilizzo Pratico

### Nel DTO del Ristorante
```java
@ValidVatNumber(allowNull = true)
@Schema(description = "International VAT/Tax number", example = "IT12345678901")
private String vatNumber;
```

### Esempi di Numeri Validi per Settore
```java
// Ristorante italiano
"IT12345678901"

// Ristorante francese
"FR12345678901"

// Catena internazionale (Australia)
"AU12345678901"

// Franchising USA
"US12-3456789"

// Ristorante brasiliano
"BR12.345.678/0001-90"
```

## Vantaggi per Greedys

1. **🌍 Espansione Globale**: Pronto per mercati internazionali
2. **✅ Compliance**: Rispetta gli standard fiscali locali
3. **🔒 Validazione Rigorosa**: Previene errori di inserimento
4. **💰 Integrazione Contabile**: Facilita integrazioni con sistemi fiscali
5. **📊 Reporting**: Migliore tracciabilità per audit e reportistica

## Migrazione Database

Lo script SQL è già pronto per aggiornare la struttura:
```sql
-- Aggiungi la nuova colonna
ALTER TABLE restaurant ADD COLUMN vat_number VARCHAR(20);

-- Copia i dati esistenti
UPDATE restaurant SET vat_number = PI WHERE PI IS NOT NULL;

-- Aggiungi indice per performance
CREATE INDEX idx_restaurant_vat_number ON restaurant(vat_number);
```

## Prossimi Passi

1. **Eseguire migrazione database**
2. **Testare con dati reali**
3. **Aggiornare documentazione API**
4. **Formare il team sul nuovo sistema**
5. **Pianificare rollout graduale**

Questo sistema di validazione delle partite IVA è ora pronto per supportare l'espansione globale di Greedys! 🚀
