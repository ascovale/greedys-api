#!/bin/bash

# Script per avviare velocemente l'app Greedys
# Autore: Script automatico
# Data: $(date)
# 
# Uso:
#   ./quick-start.sh                    - Avvia l'app
#   ./quick-start.sh stop               - Ferma l'app (mantiene DB)
#   ./quick-start.sh stop -db           - Ferma l'app e rimuove anche DB + volume

set -e  # Esce se un comando fallisce

# Colori per output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Funzione per stampare messaggi colorati
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_help() {
    echo -e "${BLUE}Uso dello script:${NC}"
    echo "  $0                    - Avvia l'app"
    echo "  $0 stop               - Ferma l'app (mantiene DB)"
    echo "  $0 stop -db           - Ferma l'app e rimuove anche DB + volume"
}

# Funzione per fermare i servizi
stop_services() {
    local remove_db=false
    
    if [[ "$2" == "-db" ]]; then
        remove_db=true
        print_warning "Modalità completa: rimuoverò anche DB e volume!"
    fi
    
    print_status "Arresto dello stack greedys_api..."
    
    if docker stack rm greedys_api; then
        print_success "Stack greedys_api rimosso con successo!"
    else
        print_error "Errore durante la rimozione dello stack"
        exit 1
    fi
    
    if [[ "$remove_db" == true ]]; then
        print_status "Attesa completamento rimozione servizi..."
        
        # Attende che tutti i servizi siano effettivamente rimossi
        while docker service ls --format "{{.Name}}" | grep -q "greedys_api"; do
            print_status "Attendo la rimozione completa dei servizi..."
            sleep 2
        done
        
        print_status "Rimozione dello stack del database..."
        if docker stack rm greedys_api_db 2>/dev/null; then
            print_success "Stack database rimosso!"
        else
            print_warning "Stack database non trovato o già rimosso"
        fi
        
        # Attende che il database sia completamente rimosso
        print_status "Attesa rimozione completa database..."
        while docker service ls --format "{{.Name}}" | grep -q "greedys_api_db"; do
            print_status "Attendo la rimozione completa del database..."
            sleep 2
        done
        
        print_status "Rimozione volume database..."
        if docker volume rm -f greedys_api_db_data 2>/dev/null; then
            print_success "Volume database rimosso!"
        else
            print_warning "Volume database non trovato o già rimosso"
        fi
        
        print_success "Pulizia completa terminata!"
    fi
    
    print_success "Operazione di stop completata!"
    exit 0
}

# Directory del progetto
PROJECT_DIR="/mnt/c/Users/ascol/Progetti/greedys_api"

# Gestione parametri
if [[ "$1" == "stop" ]]; then
    stop_services "$@"
elif [[ "$1" == "help" ]] || [[ "$1" == "-h" ]] || [[ "$1" == "--help" ]]; then
    print_help
    exit 0
elif [[ -n "$1" ]]; then
    print_error "Parametro non riconosciuto: $1"
    print_help
    exit 1
fi

# Se arriviamo qui, stiamo facendo il start
print_status "Avvio script di deployment per Greedys API..."
print_status "Directory progetto: $PROJECT_DIR"

# Cambia nella directory del progetto
print_status "Cambio nella directory del progetto..."
cd "$PROJECT_DIR" || {
    print_error "Impossibile accedere alla directory $PROJECT_DIR"
    exit 1
}

print_success "Directory cambiata correttamente"

# Build dell'immagine Docker
print_status "Inizio build dell'immagine Docker..."
print_warning "Questo processo potrebbe richiedere alcuni minuti..."

if docker buildx build -t registry.gitlab.com/psychoorange/greedys_api:latest .; then
    print_success "Build dell'immagine completata con successo!"
else
    print_error "Errore durante la build dell'immagine Docker"
    exit 1
fi

# Deploy dello stack
print_status "Deploy dello stack Docker..."

if docker stack deploy -c docker-compose.yml greedys_api; then
    print_success "Stack deployato con successo!"
else
    print_error "Errore durante il deploy dello stack"
    exit 1
fi

# Attesa per stabilizzazione dei servizi
print_status "Attesa stabilizzazione servizi (5 secondi)..."
sleep 5

# Mostra i logs del servizio
print_status "Avvio monitoraggio logs del servizio spring-app..."
print_warning "Premi Ctrl+C per interrompere il monitoraggio dei logs"

docker service logs greedys_api_spring-app -f
