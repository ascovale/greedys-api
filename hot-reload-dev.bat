@echo off
echo 🔥 Avvio DEV con HOT RELOAD (ricaricamento automatico)
echo 📝 Modifica i file Java e verranno ricaricati automaticamente
echo 📋 Profilo Maven: FULL (tutte le dipendenze)
echo.
cd greedys_api
mvn spring-boot:run -Pfull -Dspring.profiles.active=dev -DskipTests -Dmaven.test.skip=true -Dspring-boot.run.fork=false -Dspring.devtools.restart.enabled=true -Dspring.devtools.livereload.enabled=true
pause
