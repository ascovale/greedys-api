@echo off
echo 🚀 Avvio VELOCE profilo DEV (MySQL + servizi reali)
echo ⚡ Flags di ottimizzazione attivi
echo 📋 Profilo Maven: FULL (tutte le dipendenze)
echo.
cd greedys_api
mvn spring-boot:run -Pfull -Dspring.profiles.active=dev -DskipTests -Dmaven.test.skip=true -o -Dspring-boot.run.fork=false
pauseff
echo 🚀 Avvio VELOCE profilo DEV (MySQL + servizi reali)
echo ⚡ Flags di ottimizzazione attivi
echo.
cd greedys_api
mvn spring-boot:run -Dspring.profiles.active=dev -Dmaven.test.skip=true -o -Dspring-boot.run.fork=false
pause
