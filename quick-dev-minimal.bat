@echo off
echo 🚀 Avvio ULTRA-VELOCE profilo DEV-MINIMAL (H2 + mock)
echo ⚡ Flags di ottimizzazione attivi
echo 📋 Profilo Maven: MINIMAL (dipendenze essenziali)
echo.
cd greedys_api
rem Use the Spring Boot Maven Plugin property to set profiles reliably
mvn spring-boot:run -Pminimal -Dspring-boot.run.profiles=dev-minimal -DskipTests -Dmaven.test.skip=true -o -Dspring-boot.run.fork=false -Dspring.devtools.restart.enabled=false
pause
