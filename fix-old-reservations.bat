@echo off
REM Script to fix old reservations with userName via SSH on the remote server

setlocal enabledelayedexpansion

REM Server credentials (configure these)
set SERVER_IP=api.greedys.it
set SERVER_USER=deployer
set SSH_KEY=deploy\id_key

REM Database credentials (on the server)
set DB_HOST=localhost
set DB_USER=root
set DB_NAME=greedys_v1
set DB_PASSWORD=root123

echo.
echo ========================================
echo Fixing old reservations with userName
echo ========================================
echo.

REM Step 1: Check how many need fixing
echo [1/3] Checking reservations without userName...
ssh -i %SSH_KEY% %SERVER_USER%@%SERVER_IP% "mysql -h %DB_HOST% -u %DB_USER% -p%DB_PASSWORD% %DB_NAME% -e \"SELECT COUNT(*) as 'Reservations without userName' FROM reservation WHERE userName IS NULL OR userName = '';\"" 

echo.
echo [2/3] Updating reservations with default userName (Guest + ID)...

REM Step 2: Update
ssh -i %SSH_KEY% %SERVER_USER%@%SERVER_IP% "mysql -h %DB_HOST% -u %DB_USER% -p%DB_PASSWORD% %DB_NAME% -e \"UPDATE reservation SET userName = CONCAT('Guest ', id) WHERE userName IS NULL OR userName = '';\"" 

echo.
echo [3/3] Verification - checking updated reservations...

REM Step 3: Verify
ssh -i %SSH_KEY% %SERVER_USER%@%SERVER_IP% "mysql -h %DB_HOST% -u %DB_USER% -p%DB_PASSWORD% %DB_NAME% -e \"SELECT COUNT(*) as 'Reservations with userName' FROM reservation WHERE userName IS NOT NULL AND userName != '';\"" 

echo.
echo Sample of updated reservations:
ssh -i %SSH_KEY% %SERVER_USER%@%SERVER_IP% "mysql -h %DB_HOST% -u %DB_USER% -p%DB_PASSWORD% %DB_NAME% -e \"SELECT id, userName, pax, r_date FROM reservation WHERE userName LIKE 'Guest %%' LIMIT 10;\"" 

echo.
echo ✨ Done!
echo.

endlocal
