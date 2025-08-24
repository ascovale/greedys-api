@echo off
echo 🚀 Avvio ULTRA-VELOCE profilo DEV-MINIMAL (H2 + mock)
echo ⚡ Flags di ottimizzazione attivi
echo 📋 Profilo Maven: MINIMAL (dipendenze essenziali)
echo.
cd greedys_api
mvn spring-boot:run -Pminimal -Dspring.profiles.active=dev-minimal -DskipTests -Dmaven.test.skip=true -o -Dspring-boot.run.fork=false
pause
