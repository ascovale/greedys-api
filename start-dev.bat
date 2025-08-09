@echo off
echo 🚀 Avvio Greedys API - Modalità Sviluppo
echo ================================================
echo 💾 Database: H2 in memoria (~50MB RAM)
echo 🌐 Server: http://localhost:8080
echo 🗄️ H2 Console: http://localhost:8080/h2-console
echo 📖 Swagger UI: http://localhost:8080/swagger-ui.html
echo ================================================
echo 🔗 JDBC URL: jdbc:h2:mem:greedys_dev
echo 👤 Username: sa (password vuota)
echo ================================================
echo.
cd greedys_api

REM Check if dependencies are already downloaded
if not exist "target\dependency" (
    echo 📦 Download dipendenze (prima volta)...
    call mvn dependency:resolve
)

echo 🔧 Compilazione rapida...
call mvn compile -q
if %ERRORLEVEL% neq 0 (
    echo ❌ Errore durante la compilazione!
    pause
    exit /b 1
)
echo.
echo 🚀 Avvio con profilo dev (H2 database)...
call mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="-Xmx256m" -q
pause
