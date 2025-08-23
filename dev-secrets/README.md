# DEV-SECRETS - Configurazioni per esecuzione senza Docker

Questa cartella contiene i **valori di sviluppo** utilizzati in due scenari:

## 🚫 **Esecuzione SENZA Docker (standalone)**
Quando esegui l'applicazione direttamente con Java/IDE:
- `application-secrets.properties` viene caricato da Spring Boot
- L'applicazione può leggere questi valori tramite `@Value` o `Environment`

## 📁 **Struttura Files**

### Properties per Spring Boot
- `application-secrets.properties` - Configurazioni complete per Spring

### Files per Docker Secrets
- `db_password` → Docker secret `db_password_dev`
- `email_password` → Docker secret `email_password_dev`  
- `service_account` → Docker secret `service_account_dev`
- `jwt.secret` (da properties) → Docker secret `jwt_secret_dev`

## 🔄 **Workflow di utilizzo**
### Senza Docker (standalone)
```bash
# L'applicazione leggerà automaticamente application-secrets.properties
mvn spring-boot:run -Dspring.profiles.active=dev
```

## 🔒 **Sicurezza**

⚠️ **IMPORTANTE**: Questi sono valori **MOCK/LOCALI** - NON utilizzare in produzione!

- Passwords semplici (`localdev123`)
- Chiavi JWT di sviluppo
- Service Account Google mock
- API keys di test

Per produzione, utilizza Docker Swarm Secrets reali tramite `generate_secrets.sh`.
