# Admin Authentication Flow - Postman Collection

## Panoramica
Questa collection Postman è stata aggiornata per riflettere il corretto flusso di autenticazione admin in cui:

1. **Solo un admin esistente può creare nuovi admin**
2. **Solo un admin esistente può abilitare nuovi admin**
3. **Gli admin devono essere abilitati prima di poter effettuare il login**

## Flusso di Autenticazione Admin

### 1. Primary Admin Login
- **Endpoint**: `POST /admin/auth/login`
- **Scopo**: Login dell'admin primario che ha i permessi per creare e abilitare altri admin
- **Variabili usate**: `adminEmail`, `adminPassword`
- **Variabili impostate**: 
  - `adminToken` - Token JWT dell'admin primario
  - `adminRefreshToken` - Token di refresh dell'admin primario
  - `primaryAdminId` - ID dell'admin primario

### 2. Register New Admin (By Existing Admin)
- **Endpoint**: `POST /admin/register/`
- **Scopo**: Creazione di un nuovo admin da parte dell'admin primario
- **Autorizzazione**: Richiede `Bearer {{adminToken}}`
- **Variabili usate**: `newAdminFirstName`, `newAdminLastName`, `newAdminEmail`, `newAdminPassword`
- **Variabili impostate**: `newAdminId` - ID del nuovo admin creato
- **Note**: Il nuovo admin viene creato ma rimane DISABILITATO

### 3. Enable New Admin (By Existing Admin)
- **Endpoint**: `PUT /admin/{{newAdminId}}/enable`
- **Scopo**: Abilitazione del nuovo admin da parte dell'admin primario
- **Autorizzazione**: Richiede `Bearer {{adminToken}}`
- **Variabili usate**: `newAdminId`
- **Note**: Solo dopo questo step il nuovo admin può effettuare il login

### 4. New Admin Login
- **Endpoint**: `POST /admin/auth/login`
- **Scopo**: Login del nuovo admin appena abilitato
- **Variabili usate**: `newAdminEmail`, `newAdminPassword`
- **Variabili impostate**: 
  - `newAdminToken` - Token JWT del nuovo admin
  - `newAdminRefreshToken` - Token di refresh del nuovo admin

### 5. Admin Refresh Token
- **Endpoint**: `POST /admin/auth/refresh`
- **Scopo**: Rinnovo del token JWT usando il refresh token
- **Variabili usate**: `adminRefreshToken`
- **Variabili aggiornate**: `adminToken`, `adminRefreshToken`

### 6. Admin - Get Profile (Token Validation)
- **Endpoint**: `GET /admin/get`
- **Scopo**: Validazione che il token JWT funzioni correttamente
- **Autorizzazione**: Richiede `Bearer {{adminToken}}`

## Variabili Environment Richieste

### Admin Primario
- `adminEmail` - Email dell'admin primario (es. "ascolesevalentino@gmail.com")
- `adminPassword` - Password dell'admin primario (es. "Minosse100%")

### Nuovo Admin
- `newAdminFirstName` - Nome del nuovo admin (es. "Giuseppe")
- `newAdminLastName` - Cognome del nuovo admin (es. "Verdi")
- `newAdminEmail` - Email del nuovo admin (es. "giuseppe.verdi@greedys.com")
- `newAdminPassword` - Password del nuovo admin (es. "AdminPass123!")

### Token e ID (popolati automaticamente)
- `adminToken` - Token JWT dell'admin primario
- `adminRefreshToken` - Token di refresh dell'admin primario
- `primaryAdminId` - ID dell'admin primario
- `newAdminToken` - Token JWT del nuovo admin
- `newAdminRefreshToken` - Token di refresh del nuovo admin
- `newAdminId` - ID del nuovo admin

## Ordine di Esecuzione

Per testare completamente il flusso admin, eseguire le richieste in questo ordine:

1. **Primary Admin Login** - Per ottenere l'autorizzazione necessaria
2. **Register New Admin** - Per creare un nuovo admin (rimane disabilitato)
3. **Enable New Admin** - Per abilitare il nuovo admin
4. **New Admin Login** - Per verificare che il nuovo admin possa effettuare il login
5. **Admin Refresh Token** - Per testare il rinnovo del token
6. **Admin - Get Profile** - Per validare l'accesso alle API protette

## Note di Sicurezza

- Tutti gli endpoint admin richiedono autenticazione JWT (tranne il login)
- Solo admin già autenticati possono creare nuovi admin
- Solo admin già autenticati possono abilitare altri admin
- Gli admin creati sono inizialmente disabilitati e non possono effettuare il login fino all'abilitazione
- I token JWT hanno una scadenza e devono essere rinnovati usando il refresh token

## Test di Validazione

La collection include test automatici che verificano:
- Codici di stato HTTP corretti
- Presenza e validità dei token JWT
- Struttura delle risposte JSON
- Corretta memorizzazione delle variabili environment
- Funzionamento del meccanismo di refresh token

## Logs e Debugging

Ogni richiesta include logging dettagliato che mostra:
- Parametri di input utilizzati
- Token e credenziali disponibili
- Dettagli delle risposte
- Errori e messaggi di debug
- Stato delle variabili environment
