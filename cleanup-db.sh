#!/bin/bash

# Script per eliminare servizi duplicati dal database remoto
# Elimina fisicamente i servizi 152, 153, 154 dal database

echo "=== CLEANUP SERVIZI DUPLICATI ==="
echo "Connecting to remote database..."

# Esegui la pulizia in un unico comando non interattivo
ssh -o BatchMode=yes -o ConnectTimeout=10 deployer@46.101.209.92 << 'ENDSSH'
mysql -u greedys_user -p'kH8$mN9#vR2&pL5@qW7!' greedys_v1 << 'ENDSQL'
SELECT 'BEFORE CLEANUP:' as status;
SELECT id, name, active, enabled FROM service WHERE restaurant_id = 3 ORDER BY name, id;

DELETE FROM slot WHERE service_id IN (152, 153, 154);
DELETE FROM service WHERE id IN (152, 153, 154) AND restaurant_id = 3;

SELECT 'AFTER CLEANUP:' as status;  
SELECT id, name, active, enabled FROM service WHERE restaurant_id = 3 ORDER BY name, id;
SELECT COUNT(*) as remaining_services FROM service WHERE restaurant_id = 3;
ENDSQL
ENDSSH

echo "=== CLEANUP COMPLETATO ==="