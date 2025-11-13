#!/bin/bash

# Script per esecuzione rapida profilo DEV - Versione Linux/macOS
# Equivalente a dev-run.ps1

echo "🚀 Greedys API - Avvio profilo DEV"
echo "📋 Configurazione: MySQL locale + servizi reali (Firebase, Google, Twilio)"
echo ""

# Verifica che MySQL locale sia in esecuzione
echo "🔍 Verifica MySQL locale..."

# Funzione per verificare e avviare MySQL locale
check_mysql_local() {
    # Prima prova a connettersi
    if mysql -u root -pMinosseCentoXCento2025 -e "SELECT 1;" &>/dev/null; then
        echo "✅ MySQL locale è già attivo e raggiungibile!"
    else
        echo "⚠️ MySQL locale non è attivo. Tentativo di avvio..."
        
        # Prova ad avviare MySQL (diverse distribuzioni)
        if command -v systemctl &> /dev/null; then
            sudo systemctl start mysql 2>/dev/null || sudo systemctl start mysqld 2>/dev/null
        elif command -v service &> /dev/null; then
            sudo service mysql start 2>/dev/null || sudo service mysqld start 2>/dev/null
        elif command -v brew &> /dev/null; then
            brew services start mysql 2>/dev/null
        else
            echo "❌ Non riesco a trovare un comando per avviare MySQL"
            echo "   Avvia MySQL manualmente e riprova"
            return 1
        fi
        
        # Verifica di nuovo dopo il tentativo di avvio
        sleep 3
        if mysql -u root -pMinosseCentoXCento2025 -e "SELECT 1;" &>/dev/null; then
            echo "✅ MySQL locale avviato con successo!"
        else
            echo "❌ ERRORE: MySQL locale non è raggiungibile dopo il tentativo di avvio!"
            echo "   Verifica che MySQL sia installato e che la password sia corretta"
            echo "   Comando di test: mysql -u root -pMinosseCentoXCento2025 -e 'SELECT 1;'"
            return 1
        fi
    fi
        
    # Verifica/crea database greedys_dev
    mysql -u root -pMinosseCentoXCento2025 -e "CREATE DATABASE IF NOT EXISTS greedys_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null
    echo "✅ Database greedys_dev verificato/creato!"
    return 0
}

# Verifica MySQL
if check_mysql_local; then
    echo "🎯 MySQL locale pronto per l'applicazione!"
else
    echo "⚠️  Proseguo comunque, ma potrebbero esserci errori di connessione..."
fi
echo ""

# Funzione per menu
show_menu() {
    echo "Scegli modalità di esecuzione DEV:"
    echo "1) dev - Avvio standard (MySQL + servizi reali)"
    echo "2) dev-minimal - Avvio ULTRA VELOCE (H2 memoria + mock services + HOT RELOAD)"
    echo "3) dev con hot reload (ricaricamento automatico)"  
    echo "4) Solo compilazione (progress dettagliato)"
    echo "5) Pulisci cache Maven"
    echo "6) Avvio veloce (usa jar esistente, no rebuild)"
    echo "7) Compilazione VERBOSE (mostra tutti i dettagli)"
    echo "8) Ferma MySQL (Docker + locale) e esci"
    echo ""
    read -p "Inserisci numero (1-8): " choice
}

# Funzione per fermare MySQL
stop_mysql() {
    echo "🛑 Fermo MySQL..."
    
    # Ferma MySQL Docker se presente
    local docker_available=false
    if command -v docker &> /dev/null; then
        docker_available=true
    else
        echo "ℹ️  Docker non disponibile"
    fi
    
    if [ "$docker_available" = true ]; then
        if docker ps --format "table {{.Names}}" 2>/dev/null | grep -q "greedys-mysql-dev"; then
            docker stop greedys-mysql-dev
            docker rm greedys-mysql-dev
            echo "✅ MySQL Docker fermato e rimosso"
        else
            echo "ℹ️  MySQL Docker non era in esecuzione"
        fi
    fi
    
    # Ferma MySQL locale
    echo "ℹ️  Tentativo di fermare MySQL locale..."
    local service_stopped=false
    
    # Prova diversi metodi per fermare MySQL
    if command -v systemctl &> /dev/null; then
        if systemctl is-active --quiet mysql 2>/dev/null; then
            sudo systemctl stop mysql 2>/dev/null && echo "✅ MySQL locale (mysql) fermato" && service_stopped=true
        elif systemctl is-active --quiet mysqld 2>/dev/null; then
            sudo systemctl stop mysqld 2>/dev/null && echo "✅ MySQL locale (mysqld) fermato" && service_stopped=true
        fi
    elif command -v service &> /dev/null; then
        sudo service mysql stop 2>/dev/null && echo "✅ MySQL locale (mysql) fermato" && service_stopped=true ||
        sudo service mysqld stop 2>/dev/null && echo "✅ MySQL locale (mysqld) fermato" && service_stopped=true
    elif command -v brew &> /dev/null; then
        brew services stop mysql 2>/dev/null && echo "✅ MySQL locale (brew) fermato" && service_stopped=true
    fi
    
    if [ "$service_stopped" = false ]; then
        echo "ℹ️  MySQL locale non trovato o non in esecuzione"
    fi
}

# Funzione per esecuzione rapida DEV
quick_dev_run() {
    echo "⚡ Esecuzione rapida profilo DEV"
    echo "📋 Flags di ottimizzazione:"
    echo "  - Profilo Maven: FULL (tutte le dipendenze)"
    echo "  - Profilo Spring: dev (MySQL + servizi reali)"
    echo "  - Skip tests (-DskipTests -Dmaven.test.skip=true)"
    echo "  - MySQL pronto e testato ✅"
    echo ""
    echo "🔨 Avvio compilazione e esecuzione..."
    echo "   (Questo potrebbe richiedere alcuni minuti al primo avvio)"
    echo ""
    
    cd greedys_api
    mvn spring-boot:run \
        -Pfull \
        -Dspring.profiles.active=dev \
        -Dspring-boot.run.profiles=dev \
        -DskipTests \
        -Dmaven.test.skip=true \
        -Dspring-boot.run.fork=false \
        --batch-mode \
        --show-version \
        -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
}

# Funzione per esecuzione ULTRA VELOCE dev-minimal 
ultra_fast_dev_run() {
    echo "🚀 ULTRA VELOCE - Profilo dev-minimal"
    echo "📋 Configurazione ottimizzata per velocità MASSIMA:"
    echo "  - Database: H2 in memoria (no MySQL, no connessioni esterne)"
    echo "  - Servizi: TUTTI MOCK (Firebase, Google, Twilio disabilitati)"
    echo "  - Profilo Spring: dev-minimal"
    echo "  - Logging: MINIMAL (solo errori critici)"
    echo "  - HOT RELOAD: Attivo (ricaricamento automatico + LiveReload)"
    echo "  - Startup: < 30 secondi 🔥"
    echo ""
    echo "⚠️  NOTA: Questo profilo è solo per sviluppo rapido!"
    echo "   - Nessun dato persistente (H2 in memoria)"
    echo "   - Servizi esterni simulati (mock responses)"
    echo "   - Non adatto per test di integrazione"
    echo ""
    echo "🔨 Avvio ULTRA-veloce con HOT RELOAD..."
    echo ""
    
    cd greedys_api
    mvn spring-boot:run \
        -Pminimal \
        -Dspring.profiles.active=dev-minimal \
        -Dspring-boot.run.profiles=dev-minimal \
        -DskipTests \
        -Dmaven.test.skip=true \
        -Dspring-boot.run.fork=true \
        -Dspring.devtools.restart.enabled=true \
        -Dspring.devtools.livereload.enabled=true \
        -Dspring.devtools.restart.additional-paths=src/main/java \
        -Dspring.devtools.restart.poll-interval=1000 \
        -Dspring.devtools.restart.quiet-period=400 \
        --batch-mode \
        --show-version \
        -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
}

# Funzione per esecuzione con hot reload
hot_reload_dev_run() {
    echo "🔥 Hot reload attivo profilo DEV"
    echo "📋 Features:"
    echo "  - Ricaricamento automatico classi modificate"
    echo "  - LiveReload per browser"
    echo "  - Restart veloce su modifiche"
    echo "  - MySQL pronto e testato ✅"
    echo ""
    echo "🔨 Avvio compilazione con hot reload..."
    echo "   (Questo potrebbe richiedere alcuni minuti al primo avvio)"
    echo ""
    
    cd greedys_api
    mvn spring-boot:run \
        -Pfull \
        -Dspring.profiles.active=dev \
        -Dspring-boot.run.profiles=dev \
        -DskipTests \
        -Dmaven.test.skip=true \
        -Dspring-boot.run.fork=true \
        -Dspring.devtools.restart.enabled=true \
        -Dspring.devtools.livereload.enabled=true \
        -Dspring.devtools.restart.additional-paths=src/main/java \
        -Dspring.devtools.restart.poll-interval=1000 \
        -Dspring.devtools.restart.quiet-period=400 \
        --batch-mode \
        --show-version \
        -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
}

# Funzione per solo compilazione
compile_only() {
    echo "🔨 Solo compilazione..."
    echo "   (Mostra progresso dettagliato)"
    echo ""
    cd greedys_api
    mvn compile \
        -DskipTests \
        -Dmaven.test.skip=true \
        --batch-mode \
        --show-version \
        -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
}

# Funzione per pulizia cache
clean_cache() {
    echo "🧹 Pulizia cache Maven..."
    cd greedys_api
    mvn clean
    echo "✅ Cache pulita!"
}

# Funzione per compilazione verbose
verbose_compile() {
    echo "🔊 Compilazione VERBOSE - Mostra tutti i dettagli"
    echo "📋 Questo mostrerà:"
    echo "  - Progress di ogni singolo file compilato"
    echo "  - Download dipendenze con progress"
    echo "  - Dettagli completi del processo Maven"
    echo ""
    echo "⚠️  ATTENZIONE: Output molto verboso!"
    echo ""
    cd greedys_api
    mvn compile \
        -DskipTests \
        -Dmaven.test.skip=true \
        --debug \
        --show-version \
        -X
}

# Funzione per avvio veloce senza rebuild
quick_jar_run() {
    echo "⚡ Avvio veloce senza rebuild"
    echo "📋 Usa il jar già compilato in target/"
    echo ""
    echo "Scegli profilo Spring:"
    echo "1) dev (MySQL + servizi reali)"
    echo "2) dev-minimal (H2 + mock services)"
    read -p "Profilo (1-2): " profile_choice
    
    cd greedys_api
    
    # Controlla se esiste il jar
    if [ ! -f target/greedys_api-0.1.1.jar ]; then
        echo "❌ JAR non trovato! Compila prima con l'opzione 4"
        echo "   Oppure usa l'opzione 1 per compilare e avviare"
        return 1
    fi
    
    case $profile_choice in
        1)
            echo "🚀 Avvio JAR esistente con profilo DEV..."
            java -jar target/greedys_api-0.1.1.jar --spring.profiles.active=dev
            ;;
        2)
            echo "🚀 Avvio JAR esistente con profilo DEV-MINIMAL..."
            java -jar target/greedys_api-0.1.1.jar --spring.profiles.active=dev-minimal
            ;;
        *)
            echo "❌ Scelta profilo non valida!"
            ;;
    esac
}

# Menu principale
show_menu

case $choice in
    1)
        quick_dev_run
        ;;
    2)
        ultra_fast_dev_run
        ;;
    3)
        hot_reload_dev_run
        ;;
    4)
        compile_only
        ;;
    5)
        clean_cache
        ;;
    6)
        quick_jar_run
        ;;
    7)
        verbose_compile
        ;;
    8)
        stop_mysql
        ;;
    *)
        echo "❌ Scelta non valida!"
        ;;
esac
