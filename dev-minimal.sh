#!/bin/bash

# =============================================================================
# 🚀 DEV-MINIMAL.SH - Quick Start per sviluppo locale con H2
# =============================================================================
# Esegue l'applicazione in modalità dev-minimal con hot reload
# Nessuna password richiesta, nessun Docker
# Hot Reload: Spring DevTools + fork mode per restart automatico
# =============================================================================

cd "$(dirname "$0")"

echo ""
echo "🚀 =============================================="
echo "   GREEDYS API - DEV MINIMAL MODE"
echo "   Hot Reload Abilitato | Database H2 In-Memory"
echo "==============================================="
echo ""
echo "💡 Hot Reload attivo: modifica i file e salva!"
echo "   Il server si riavvierà automaticamente."
echo ""

# Esegui Maven con profilo dev-minimal e spring-boot:run
# fork=true abilita DevTools hot reload
mvn -f greedys_api/pom.xml spring-boot:run \
    -Dspring-boot.run.profiles=dev-minimal \
    -Dspring-boot.run.fork=true \
    -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m -Dspring.devtools.restart.enabled=true -Dspring.devtools.livereload.enabled=true" \
    -DskipTests
