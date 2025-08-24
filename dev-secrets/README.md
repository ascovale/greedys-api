# DEV-SECRETS - File per esecuzione senza Docker

Questa cartella contiene i **file secret di sviluppo** utilizzati in due scenari:

## 🚫 **Esecuzione SENZA Docker (standalone)**
Quando esegui l'applicazione direttamente con Java/IDE:
- L'applicazione legge i secret da questi file tramite `file:./dev-secrets/filename`
- Utilizzato dai profili `application.properties` (normale) e `application-dev.properties`

## 📁 **Struttura Files**

### File Secret per Spring Boot (standalone)
- `jwt_secret` → JWT secret per autenticazione
- `db_password` → Password database MySQL  
- `email_password` → Password per SMTP email
- `google_oauth_client_secret` → Google OAuth Client Secret
- `service_account` → File JSON Service Account Google/Firebase
- `twilio_account_sid` → Twilio Account SID (NEW)
- `twilio_auth_token` → Twilio Auth Token (NEW)
- `twilio_verify_service_sid` → Twilio Verify Service SID (NEW)

### Files utilizzati anche per Docker Secrets  
Gli stessi file vengono copiati come Docker secrets per i container:
- `db_password` → Docker secret `db_password_dev`
- `email_password` → Docker secret `email_password_dev`  
- `service_account` → Docker secret `service_account_dev`
- `jwt_secret` → Docker secret `jwt_secret_dev`

## 🔄 **Workflow di utilizzo**

### Senza Docker (profilo normale)
```bash
# Usa MySQL esterno + secret da file
mvn spring-boot:run
```

### Con profilo `dev` (TUTTE le dipendenze + MySQL)
```bash
# Usa MySQL locale + servizi reali (Firebase, Google, Twilio) + secret da file
mvn spring-boot:run -Dspring.profiles.active=dev
```

### Con profilo `dev-minimal` (SOLO dipendenze essenziali + H2)
```bash
# Usa H2 + mock services + JWT hardcoded
mvn spring-boot:run -Dspring.profiles.active=dev-minimal
```

## 🔒 **Sicurezza**

⚠️ **IMPORTANTE**: Questi sono valori **MOCK/LOCALI** - NON utilizzare in produzione!

- Passwords semplici (`localdev123`)
- Chiavi JWT di sviluppo
- Service Account Google mock
- API keys di test

Per produzione, utilizza Docker Swarm Secrets reali tramite `generate_secrets.sh`.
