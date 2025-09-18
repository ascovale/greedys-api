# Greedy's API - Enhanced Postman Collections

## 📋 Panoramica delle Collezioni Potenziate

Questo aggiornamento include nuove collezioni Postman per test completi di sicurezza, performance e automazione per l'API di Greedy's Restaurant Management System.

### 🆕 Nuove Collezioni Aggiunte

#### 1. **Security-RoleAccess-Collection.json**
- **Focus**: Test di sicurezza e controllo accessi basato sui ruoli
- **Caratteristiche**:
  - Flussi di autenticazione completi per CUSTOMER, RUSER, ADMIN
  - Test di autorizzazione cross-role
  - Validazione token JWT e gestione scadenza
  - Test di prevenzione SQL injection e XSS
  - Scenari di sicurezza negativi

#### 2. **Performance-LoadTest-Collection.json**
- **Focus**: Test di performance e carico
- **Caratteristiche**:
  - Monitoraggio tempi di risposta con soglie configurabili
  - Simulazione utenti concorrenti
  - Test di carico pesante per database
  - Metriche di performance dettagliate
  - Scenari di stress testing

#### 3. **greedys-environment-enhanced.json**
- **Focus**: Environment potenziato per tutti i tipi di test
- **Caratteristiche**:
  - 150+ variabili preconfigurate
  - Support multi-environment (dev, staging, prod)
  - Variabili per performance testing
  - Configurazioni di automazione avanzate
  - Gestione token e cleanup automatico

---

## 🚀 Guida Rapida all'Utilizzo

### Prerequisiti
1. **Postman** installato (versione 10.x o superiore)
2. **API Greedy's** in esecuzione su `https://172.17.48.81:5050`
3. **Account admin** configurato: `ascolesevalentino@gmail.com`

### Importazione delle Collezioni

#### Passo 1: Import Environment
```bash
# Importa l'environment potenziato
Postman → Import → greedys-environment-enhanced.json
```

#### Passo 2: Import Collezioni
```bash
# Importa tutte le collezioni potenziate
Postman → Import → Security-RoleAccess-Collection.json
Postman → Import → Performance-LoadTest-Collection.json

# Le collezioni originali rimangono disponibili:
Admin-API-Collection.json
Customer-API-Collection.json  
Restaurant-API-Collection.json
```

#### Passo 3: Configurazione Environment
1. Seleziona "Greedy's Restaurant Enhanced Testing Environment"
2. Verifica che `baseUrl` sia impostato correttamente
3. Le credenziali admin sono già preconfigurate

---

## 🔐 Utilizzo della Security Collection

### Test di Sicurezza Completi
La **Security-RoleAccess-Collection** include:

#### 🎯 Scenario 1: Multi-Role Authentication
- Registrazione e login Customer
- Login Restaurant User  
- Login Admin
- Gestione automatica token JWT

#### 🎯 Scenario 2: Role-Based Authorization
- ✅ Test accesso autorizzato per ogni ruolo
- ❌ Test accesso negato cross-role
- Validazione permessi granulari

#### 🎯 Scenario 3: Token Security
- Test token malformati
- Test token scaduti
- Test senza authorization header

#### 🎯 Scenario 4: Input Validation Security
- Prevenzione SQL injection
- Sanitizzazione XSS
- Validazione dati input

### Esecuzione Test Sicurezza
```bash
# Via Postman Runner
Run Collection → Security-RoleAccess-Collection
Environment → Greedy's Restaurant Enhanced Testing Environment
Iterations → 1
Delay → 100ms

# Via Newman (CLI)
newman run Security-RoleAccess-Collection.json \
  -e greedys-environment-enhanced.json \
  --reporters cli,html \
  --reporter-html-export security-results.html
```

---

## 📊 Utilizzo della Performance Collection  

### Test di Performance Avanzati
La **Performance-LoadTest-Collection** include:

#### 🎯 Benchmark Performance
- **Soglie configurabili**:
  - Fast: < 1000ms
  - Acceptable: < 3000ms  
  - Maximum: < 5000ms

#### 🎯 Scenari di Test
1. **Authentication Performance**: Login speed sotto carico
2. **API Endpoints Performance**: Response time per operazioni critiche
3. **Concurrent Load Testing**: Simulazione utenti simultanei
4. **Heavy Data Load**: Test performance con dataset grandi

#### 🎯 Metriche Monitorate
- Response time per request
- Throughput (richieste/secondo)
- Dimensione response
- Performance ratio (tempo/record)

### Esecuzione Performance Tests

#### Test Singolo
```bash
# Performance standard
Run Collection → Performance-LoadTest-Collection
Iterations → 1
Environment → Enhanced Testing Environment
```

#### Load Testing con Newman
```bash
# Test carico con 50 iterazioni e 10 utenti concorrenti
newman run Performance-LoadTest-Collection.json \
  -e greedys-environment-enhanced.json \
  --iteration-count 50 \
  --parallel 10 \
  --delay-request 100 \
  --reporters cli,json \
  --reporter-json-export performance-results.json
```

#### Analisi Performance
```bash
# Generazione report dettagliato
newman run Performance-LoadTest-Collection.json \
  -e greedys-environment-enhanced.json \
  --iteration-count 100 \
  --reporters htmlextra \
  --reporter-htmlextra-export performance-report.html
```

---

## ⚙️ Configurazioni Avanzate

### Variabili Environment Chiave

#### Base Configuration
```json
{
  "baseUrl": "https://172.17.48.81:5050",
  "baseUrlDev": "http://localhost:8080",
  "baseUrlStaging": "https://staging.greedy.api.com"
}
```

#### Performance Thresholds  
```json
{
  "maxResponseTime": "5000",
  "fastResponseTime": "1000", 
  "slowResponseTime": "3000",
  "concurrentUsers": "10",
  "loadTestIterations": "100"
}
```

#### Test Control
```json
{
  "debugMode": "false",
  "securityTestMode": "false", 
  "performanceMonitoring": "true",
  "testDataCleanup": "true"
}
```

### Automazione CI/CD

#### GitHub Actions Example
```yaml
name: API Tests
on: [push, pull_request]
jobs:
  api-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Install Newman
        run: npm install -g newman newman-reporter-htmlextra
      
      - name: Security Tests
        run: |
          newman run test-postman/Security-RoleAccess-Collection.json \
            -e test-postman/greedys-environment-enhanced.json \
            --reporters cli,htmlextra \
            --reporter-htmlextra-export security-report.html
      
      - name: Performance Tests
        run: |
          newman run test-postman/Performance-LoadTest-Collection.json \
            -e test-postman/greedys-environment-enhanced.json \
            --iteration-count 10 \
            --reporters cli,htmlextra \
            --reporter-htmlextra-export performance-report.html
```

---

## 🎯 Best Practices per Test Execution

### 1. **Ordine di Esecuzione Raccomandato**
1. Environment setup e health check
2. Security tests (autenticazione base)
3. Functional tests (collezioni originali)  
4. Performance tests
5. Advanced security tests
6. Cleanup

### 2. **Monitoraggio Performance**
- Abilitare `performanceMonitoring: true`
- Controllare logs dettagliati in Console
- Usare Newman per metriche precise
- Comparare risultati tra ambienti

### 3. **Gestione Test Data**
- I test generano dati temporanei con ID univoci
- Cleanup automatico abilitato di default
- Separazione dati per environment diversi

### 4. **Debugging e Troubleshooting**
```json
{
  "debugMode": "true",           // Logging esteso
  "securityTestMode": "true",    // Validazioni extra sicurezza  
  "performanceMonitoring": "true" // Metriche dettagliate
}
```

---

## 📈 Reporting e Analytics

### Newman HTML Reports
I report HTML generati includono:
- **Summary Dashboard**: Overview test results
- **Request Timeline**: Performance waterfall
- **Failed Requests**: Error analysis
- **Performance Metrics**: Response time distribution

### Custom Logging
Le collezioni includono logging personalizzato:
- 🔐 Security validation logs
- 📊 Performance metrics logs  
- ⚡ Real-time performance classification
- 🧹 Cleanup operation logs

---

## 🔧 Personalizzazione e Estensioni

### Aggiungere Nuovi Test
1. Duplicare un test esistente simile
2. Modificare endpoint e payload
3. Aggiornare test assertions
4. Aggiungere variabili environment se necessario

### Modificare Soglie Performance
```json
{
  "maxResponseTime": "3000",      // Ridurre per API critiche
  "fastResponseTime": "500",      // Aumentare per operazioni complesse
  "concurrentUsers": "20"         // Aumentare per stress testing
}
```

### Environment Switching
```json
// Development
"baseUrl": "http://localhost:8080"

// Staging  
"baseUrl": "https://staging.greedy.api.com"

// Production
"baseUrl": "https://api.greedy.com"
```

---

## 📞 Supporto e Troubleshooting

### Problemi Comuni

#### Token Scaduto
```
Error: 401 Unauthorized
Soluzione: Re-run authentication requests
```

#### Performance Timeout
```
Error: Response time > maxResponseTime
Soluzione: Aumentare soglie o ottimizzare API
```

#### Environment Variables Missing
```
Error: Variable not found
Soluzione: Re-import enhanced environment
```

### Logs di Debug
Abilitare debug mode per logging dettagliato:
```json
{
  "debugMode": "true"
}
```

---

## 🏆 Risultati Attesi

Dopo l'implementazione, dovresti avere:

✅ **3 nuove collezioni** per test completi  
✅ **150+ variabili** environment preconfigurate  
✅ **Security testing** automatizzato con 20+ scenari  
✅ **Performance monitoring** con soglie configurabili  
✅ **Load testing** con simulazione utenti concorrenti  
✅ **Reporting avanzato** via Newman HTML  
✅ **CI/CD ready** per automazione completa  

Le collezioni esistenti rimangono **completamente funzionali** e possono essere usate insieme alle nuove per una copertura di test al 100%.

---

*💡 **Suggerimento**: Esegui prima i test su ambiente di sviluppo per familiarizzare con le nuove funzionalità prima di usarle su staging/production.*