@echo off
REM Script di avvio rapido per sviluppo Windows
REM Ottimizzato per ridurre i tempi di compilazione

echo 🚀 Avvio rapido modalità sviluppo...

cd greedys_api

REM Verifica se è necessaria una compilazione completa
if not exist "target\classes" (
    echo 📦 Prima compilazione necessaria...
    mvn clean compile -DskipTests -q
) else if not exist "target\classes\application.properties" (
    echo 📦 Prima compilazione necessaria...
    mvn clean compile -DskipTests -q
) else (
    echo ♻️  Usando cache esistente, compilazione incrementale...
    mvn compile -DskipTests -q
)

echo 🔥 Avvio con hot reload attivato...

REM Avvia con hot reload e JVM ottimizzata
mvn spring-boot:run ^
    -Dspring-boot.run.profiles=dev ^
    -DskipTests ^
    -Dspring-boot.run.jvmArguments="-Xmx512m -Xms256m -Dspring.devtools.restart.enabled=true -Dspring.devtools.livereload.enabled=true" ^
    -Dspring-boot.run.fork=true ^
    -q
