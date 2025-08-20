#!/bin/bash

# Script per fermare l'app Greedys
# Autore: Script automatico
# Data: $(date)
# 
# Uso:
#   ./stop.sh                    - Ferma l'app (mantiene DB)
#   ./stop.sh -db                - Ferma l'app e rimuove anche DB + volume

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
    echo -e "${BLUE}Uso dello script stop:${NC}"
    echo "  $0                    - Ferma l'app (mantiene DB)"
    echo "  $0 -db                - Ferma l'app e rimuove anche DB + volume"
    echo "  $0 help               - Mostra questo aiuto"
}

# Funzione per fermare i servizi
stop_services() {
    local remove_db=false
    
    if [[ "$1" == "-db" ]]; then
        remove_db=true
        print_warning "Modalità completa: rimuoverò anche DB e volume!"
        echo -n "Sei sicuro? (y/N): "
        read -r confirmation
        if [[ ! "$confirmation" =~ ^[Yy]$ ]]; then
            print_status "Operazione annullata dall'utente"
            exit 0
        fi
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
        
        # Pulizia aggiuntiva - rimuove eventuali container orfani
        print_status "Pulizia container orfani..."
        if docker container prune -f >/dev/null 2>&1; then
            print_success "Container orfani rimossi!"
        fi
        
        # Pulizia network orfani
        print_status "Pulizia network orfani..."
        if docker network prune -f >/dev/null 2>&1; then
            print_success "Network orfani rimossi!"
        fi
        
        print_success "Pulizia completa terminata!"
    fi
    
    print_success "Operazione di stop completata!"
}

# Gestione parametri
if [[ "$1" == "-db" ]]; then
    stop_services "-db"
elif [[ "$1" == "help" ]] || [[ "$1" == "-h" ]] || [[ "$1" == "--help" ]]; then
    print_help
    exit 0
elif [[ -n "$1" ]]; then
    print_error "Parametro non riconosciuto: $1"
    print_help
    exit 1
else
    stop_services
fi
