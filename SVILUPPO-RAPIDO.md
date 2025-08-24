# 🚀 Comandi di Sviluppo Rapido

Questo progetto include diversi script per velocizzare lo sviluppo evitando di ricaricare le dipendenze ogni volta.

## 📋 File di Esecuzione Rapida

### **Windows (.bat files)**
- `quick-dev.bat` - DEV con MySQL + servizi reali (VELOCE)
- `quick-dev-minimal.bat` - DEV-MINIMAL con H2 + mock (ULTRA-VELOCE)  
- `hot-reload-dev.bat` - DEV con ricaricamento automatico

### **Cross-platform (.sh e .ps1)**
- `dev-run.sh` - Script bash con menu interattivo
- `dev-run.ps1` - Script PowerShell con menu interattivo

## ⚡ Ottimizzazioni Implementate

### **Spring Boot DevTools**
- ✅ Aggiunto al `pom.xml`
- 🔥 Ricaricamento automatico delle classi modificate
- 📱 LiveReload per browser
- ⚡ Restart veloce dell'applicazione

### **Flags Maven Ottimizzati**
- `-DskipTests` - Salta i test durante l'esecuzione
- `-o` - Modalità offline (non ricarica dipendenze)
- `-Dspring-boot.run.fork=false` - Esecuzione nello stesso processo
- `-Dspring.devtools.restart.enabled=true` - Abilita hot reload

## 🚀 Comandi Manuali Veloci

### **Profilo DEV (MySQL + servizi reali) - VELOCE:**
```bash
mvn spring-boot:run -Dspring.profiles.active=dev -DskipTests -o
```

### **Profilo DEV-MINIMAL (H2 + mock) - ULTRA-VELOCE:**
```bash
mvn spring-boot:run -Dspring.profiles.active=dev-minimal -DskipTests -o
```

### **Con Hot Reload (ricaricamento automatico):**
```bash
mvn spring-boot:run -Dspring.profiles.active=dev -DskipTests -Dspring.devtools.restart.enabled=true
```

### **Solo compilazione (senza esecuzione):**
```bash
mvn compile -DskipTests -o
```

## 🔄 Workflow di Sviluppo Consigliato

### **Prima volta (setup completo):**
```bash
mvn clean install
```

### **Sviluppo giornaliero (veloce):**
1. **Windows**: Doppio click su `quick-dev.bat` o `quick-dev-minimal.bat`
2. **Linux/Mac**: `./dev-run.sh`
3. **PowerShell**: `./dev-run.ps1`

### **Con modifiche frequenti:**
- Usa `hot-reload-dev.bat` per ricaricamento automatico
- Modifica i file Java e verranno ricompilati automaticamente

## 📊 Tempi di Avvio Stimati

| Modalità | Primo Avvio | Avvii Successivi |
|----------|-------------|------------------|
| `mvn spring-boot:run` | ~2-3 min | ~2-3 min |
| `quick-dev.bat` | ~2-3 min | ~30-45 sec |
| `quick-dev-minimal.bat` | ~1-2 min | ~15-30 sec |
| Hot Reload | ~30-45 sec | ~5-10 sec |

## 💡 Tips per Sviluppo

1. **Usa dev-minimal per modifiche frontend/controller**
2. **Usa dev per testare servizi esterni (Google, Twilio)**
3. **Hot reload è perfetto per modifiche frequenti**
4. **Offline mode (-o) funziona solo se le dipendenze sono già scaricate**

## 🔧 Troubleshooting

### **Se il comando fallisce:**
```bash
mvn clean install  # Ricarica tutto
```

### **Se hot reload non funziona:**
- Controlla che Spring DevTools sia attivo nei log
- Riavvia l'IDE se necessario

### **Se database non si connette:**
- Verifica che MySQL sia avviato (per profilo dev)
- Controlla password in `dev-secrets/db_password`
