# Validazione Internazionale della Partita IVA

## Panoramica

Il sistema di validazione delle partite IVA implementato nel progetto Greedys API supporta il formato **VIES (VAT Information Exchange System)** dell'Unione Europea e altri standard internazionali.

## Formato Standard

Le partite IVA devono seguire il formato internazionale:
- **2 lettere** del codice paese (ISO 3166-1 alpha-2)
- **Numero di partita IVA nazionale** secondo le regole del paese

### Esempi per Paese

#### Paesi Principali UE
- **Italia**: `IT12345678901` (IT + 11 cifre)
- **Francia**: `FR12345678901` (FR + 2 caratteri + 9 cifre)
- **Germania**: `DE123456789` (DE + 9 cifre)
- **Spagna**: `ESA12345674` (ES + 1 lettera + 7 cifre + 1 alfanumerico)
- **Paesi Bassi**: `NL123456789B01` (NL + 9 cifre + B + 2 cifre)
- **Belgio**: `BE1234567890` (BE + 10 cifre)

#### Altri Paesi Europei
- **Austria**: `ATU12345678` (ATU + 8 cifre)
- **Portogallo**: `PT123456789` (PT + 9 cifre)
- **Grecia**: `EL123456789` o `GR123456789` (EL/GR + 9 cifre)
- **Irlanda**: `IE1234567A` o `IE1A23456B` (formati multipli)
- **Regno Unito**: `GB123456789` (GB + 9 o 12 cifre, o formati speciali)
- **Svizzera**: `CHE123456789MWST` (CHE + 9 cifre + suffisso)
- **Norvegia**: `NO123456789MVA` (NO + 9 cifre + MVA)

## Implementazione

### 1. Annotation @ValidVatNumber

```java
@ValidVatNumber
private String vatNumber;

// Con configurazioni personalizzate
@ValidVatNumber(allowNull = false, message = "Partita IVA obbligatoria")
private String requiredVatNumber;
```

### 2. Utilizzo nei DTO

```java
@ValidVatNumber(allowNull = true)
@Schema(description = "International VAT number following VIES format", 
        example = "IT12345678901", 
        pattern = "^[A-Z]{2}[A-Z0-9]+$")
private String vatNumber;
```

### 3. Nel Modello Restaurant

Il campo `vatNumber` sostituisce il vecchio campo `pI`, mantenendo la compatibilità:

```java
@Column(name = "vat_number", length = 20)
private String vatNumber; // International VAT number (e.g., IT12345678901)

// Metodi per retrocompatibilità
@Deprecated
public String getPI() { return this.vatNumber; }

@Deprecated  
public void setPI(String pI) { this.vatNumber = pI; }
```

## Caratteristiche del Validator

### Controlli Implementati

1. **Formato del codice paese**: Verifica che i primi 2 caratteri siano un codice paese supportato
2. **Pattern specifico per paese**: Ogni paese ha il suo formato specifico
3. **Lunghezza**: Controllo della lunghezza secondo le regole nazionali
4. **Caratteri validi**: Solo lettere e numeri nelle posizioni appropriate
5. **Normalizzazione**: Rimuove automaticamente spazi e trattini

### Paesi Supportati

Il validator supporta **29 paesi europei**:
- Tutti i 27 paesi UE
- Regno Unito (post-Brexit)
- Svizzera
- Norvegia

### Gestione degli Errori

Il validator fornisce messaggi di errore specifici:
- Codice paese non supportato
- Formato non valido per il paese specifico
- Lunghezza incorretta
- Caratteri non validi

## Migrazione Database

Per aggiornare il database esistente:

```sql
-- Aggiunge la nuova colonna
ALTER TABLE restaurant ADD COLUMN vat_number VARCHAR(20);

-- Copia i dati dal vecchio campo
UPDATE restaurant SET vat_number = PI WHERE PI IS NOT NULL;

-- Aggiunge indice per performance
CREATE INDEX idx_restaurant_vat_number ON restaurant(vat_number);

-- Rimuove il vecchio campo (quando pronto)
-- ALTER TABLE restaurant DROP COLUMN PI;
```

## Messaggi di Validazione

I messaggi sono localizzati in 3 lingue:

- **Italiano**: `Numero di partita IVA non valido. Utilizzare il formato internazionale (es. IT12345678901)`
- **Inglese**: `Invalid VAT number. Use international format (e.g. IT12345678901)`
- **Spagnolo**: `Número de IVA inválido. Use formato internacional (ej. ES12345678A)`

## Test

La classe `VatNumberValidatorTest` fornisce test completi per:
- Tutti i formati nazionali supportati
- Gestione di valori null/vuoti
- Normalizzazione dell'input (spazi, trattini, case)
- Messaggi di errore personalizzati
- Casi limite e formati non validi

## Benefici

1. **Standard Internazionale**: Segue il formato VIES riconosciuto in Europa
2. **Validazione Robusta**: Controlli specifici per ogni paese
3. **Retrocompatibilità**: Mantiene il funzionamento del codice esistente
4. **Facilità d'uso**: Annotation semplice da applicare
5. **Localizzazione**: Messaggi di errore in più lingue
6. **Performance**: Validazione veloce tramite regex ottimizzate
7. **Estensibilità**: Facile aggiungere nuovi paesi

## Esempi d'Uso

```java
// DTO di registrazione ristorante
public class NewRestaurantDTO {
    @ValidVatNumber(allowNull = true)
    private String vatNumber;
}

// Modello Restaurant  
public class Restaurant {
    @Column(name = "vat_number")
    private String vatNumber;
}

// Validazione programmatica
ValidVatNumber annotation = field.getAnnotation(ValidVatNumber.class);
VatNumberValidator validator = new VatNumberValidator();
validator.initialize(annotation);
boolean isValid = validator.isValid("IT12345678901", context);
```

Questa implementazione garantisce che le partite IVA siano validate secondo gli standard internazionali, migliorando la qualità dei dati e facilitando le operazioni commerciali transfrontaliere.
