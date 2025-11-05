-- Script per eliminare servizi duplicati dal database
-- Eliminiamo i servizi NUOVI (152, 153, 154) e manteniamo quelli VECCHI (102, 103, 104)

-- Prima verifichiamo la situazione attuale
SELECT 'BEFORE CLEANUP - Services for restaurant 3:' as status;
SELECT id, name, active, enabled, restaurant_id, valid_from, valid_to 
FROM service 
WHERE restaurant_id = 3 
ORDER BY name, id;

-- Controlliamo se ci sono slot associati ai servizi che vogliamo eliminare
SELECT 'Checking slots for services 152, 153, 154:' as status;
SELECT s.id as service_id, s.name as service_name, sl.id as slot_id, sl.start_time, sl.end_time, sl.weekday
FROM service s 
LEFT JOIN slot sl ON s.id = sl.service_id
WHERE s.id IN (152, 153, 154)
ORDER BY s.id;

-- Controlliamo se ci sono prenotazioni associate (SAFETY CHECK)
SELECT 'Checking reservations for services 152, 153, 154:' as status;
SELECT COUNT(*) as reservation_count, s.id as service_id, s.name
FROM reservation r
JOIN slot sl ON r.id_slot = sl.id
JOIN service s ON sl.service_id = s.id
WHERE s.id IN (152, 153, 154)
GROUP BY s.id, s.name;

-- Se non ci sono prenotazioni, procediamo con l'eliminazione
-- STEP 1: Eliminiamo prima eventuali slot associati ai servizi 152, 153, 154
DELETE FROM slot WHERE service_id IN (152, 153, 154);

-- STEP 2: Eliminiamo i servizi duplicati 152, 153, 154
DELETE FROM service WHERE id IN (152, 153, 154) AND restaurant_id = 3;

-- Verifichiamo il risultato finale
SELECT 'AFTER CLEANUP - Services for restaurant 3:' as status;
SELECT id, name, active, enabled, restaurant_id, valid_from, valid_to 
FROM service 
WHERE restaurant_id = 3 
ORDER BY name, id;

-- Contiamo i servizi rimanenti per conferma
SELECT 'Final count:' as status, COUNT(*) as total_services,
       COUNT(CASE WHEN name = 'Colazione' THEN 1 END) as colazione_count,
       COUNT(CASE WHEN name = 'Pranzo' THEN 1 END) as pranzo_count,
       COUNT(CASE WHEN name = 'Cena' THEN 1 END) as cena_count
FROM service 
WHERE restaurant_id = 3;