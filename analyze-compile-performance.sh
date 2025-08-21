#!/bin/bash

# ============================================================================
# ANALISI PERFORMANCE: DOCKER vs LOCAL COMPILATION
# Script per confrontare i tempi di compilazione
# ============================================================================

set -e

echo "🔍 ===== ANALISI PERFORMANCE COMPILATION ====="
echo ""

# Vai nella directory del progetto
cd "$(dirname "$0")/greedys_api"
PROJECT_DIR=$(pwd)

echo "📁 Directory progetto: $PROJECT_DIR"
echo "🕐 Timestamp inizio: $(date)"
echo ""

# ============================================================================
# FUNZIONE: Analizza configurazione ambiente
# ============================================================================
analyze_environment() {
    echo "🖥️  ANALISI AMBIENTE:"
    echo "   🐧 OS: $(uname -a)"
    echo "   ☕ Java: $(java -version 2>&1 | head -1)"
    echo "   📦 Maven: $(mvn -version | head -1)"
    echo "   💾 RAM disponibile: $(free -h | grep Mem | awk '{print $7}')"
    echo "   💿 Spazio disco: $(df -h . | tail -1 | awk '{print $4}')"
    echo ""
}

# ============================================================================
# FUNZIONE: Test compilazione locale
# ============================================================================
test_local_compilation() {
    echo "🏠 TEST COMPILAZIONE LOCALE:"
    echo "   🧹 Pulizia cache Maven..."
    rm -rf ~/.m2/repository/com/application 2>/dev/null || true
    
    echo "   ⏱️  Inizio compilazione locale..."
    start_time=$(date +%s)
    
    # Compilazione con profilo minimal
    echo "   📦 Comando: mvn clean compile -Pminimal"
    mvn clean compile -Pminimal -X > compile_local.log 2>&1
    local_result=$?
    
    end_time=$(date +%s)
    local_duration=$((end_time - start_time))
    
    if [ $local_result -eq 0 ]; then
        echo "   ✅ Successo in ${local_duration}s"
    else
        echo "   ❌ Fallimento dopo ${local_duration}s"
        echo "   📋 Log errore: compile_local.log"
    fi
    
    # Analisi log
    echo "   📊 Analisi log compilazione:"
    if [ -f "compile_local.log" ]; then
        echo "      🔄 Download dipendenze:"
        grep -c "Downloading" compile_local.log || echo "      📦 0 nuovi download"
        echo "      ⚡ Classi compilate:"
        grep -c "Compiling.*source" compile_local.log || echo "      📝 0 classi trovate nel log"
        echo "      ⚠️  Warning:"
        grep -c "WARNING" compile_local.log || echo "      ✅ 0 warning"
        echo "      📏 Dimensione log: $(wc -l < compile_local.log) righe"
    fi
    
    echo ""
}

# ============================================================================
# FUNZIONE: Analizza compilazione Docker (simulata)
# ============================================================================
analyze_docker_compilation() {
    echo "🐳 ANALISI COMPILAZIONE DOCKER:"
    echo "   📋 Come funziona Docker build:"
    echo "      🏗️  Build context: Usa cache Docker layer"
    echo "      📦 Dependencies: Cache ~/.m2 in Docker layer"
    echo "      🚀 JVM: JVM ottimizzata per container"
    echo "      💿 Storage: Filesystem Docker (overlay2)"
    echo ""
    
    echo "   🔍 Differenze chiave vs Local:"
    echo "      📚 Cache Maven: Docker mantiene ~/.m2 tra build"
    echo "      🏃 JVM Args: Docker usa JVM args ottimizzati"
    echo "      💾 Memory: Docker limita memory per JVM"
    echo "      🔧 Profile: Docker usa 'full' (non minimal)"
    echo ""
    
    # Simula analisi Dockerfile
    if [ -f "../Dockerfile" ]; then
        echo "   📄 Analisi Dockerfile:"
        echo "      🐳 Base image: $(grep "FROM" ../Dockerfile | head -1)"
        echo "      📦 Maven args: $(grep "ARG MAVEN_PROFILES" ../Dockerfile || echo 'Default (nessun profilo)')"
        echo "      💾 JVM memory: $(grep -E "(Xmx|Xms)" ../Dockerfile || echo 'Default JVM settings')"
    fi
    echo ""
}

# ============================================================================
# FUNZIONE: Raccomandazioni ottimizzazione
# ============================================================================
provide_optimizations() {
    echo "🚀 RACCOMANDAZIONI OTTIMIZZAZIONE:"
    echo ""
    
    echo "   💡 Per velocizzare compilazione locale:"
    echo "      📦 MAVEN_OPTS=\"-Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication\""
    echo "      🔧 mvn -T 4 compile (parallel build)"
    echo "      📚 mvn dependency:go-offline -Pminimal (cache dependencies)"
    echo "      ⚡ mvn compile -o (offline mode dopo cache)"
    echo ""
    
    echo "   🎯 Test JVM ottimizzato:"
    echo "      export MAVEN_OPTS=\"-Xmx2g -XX:+UseG1GC\""
    echo "      mvn clean compile -Pminimal -T 2"
    echo ""
    
    echo "   🐳 Come replicare performance Docker:"
    echo "      📦 Usa stesso profilo: mvn compile (senza -Pminimal)"
    echo "      💾 Cache dependencies: mvn dependency:go-offline"
    echo "      🔧 JVM settings: export MAVEN_OPTS=\"-Xmx1g -Xms512m\""
    echo ""
}

# ============================================================================
# ESECUZIONE ANALISI
# ============================================================================

analyze_environment
test_local_compilation
analyze_docker_compilation
provide_optimizations

echo "🎯 ===== RISULTATI ANALISI ====="
echo "📊 Tempo compilazione locale: ${local_duration}s"
echo "📋 Log dettagliato: compile_local.log"
echo ""
echo "💡 PROSSIMI PASSI:"
echo "   1. Controlla compile_local.log per dettagli"
echo "   2. Prova ottimizzazioni JVM suggerite"
echo "   3. Confronta con Docker build time"
echo ""
echo "🔍 Per vedere differenze specifiche:"
echo "   tail -50 compile_local.log"
echo "   grep -E '(Downloading|Downloaded)' compile_local.log"
