# 30 Monday Customer Reservations Collection

## Descrizione
Questa collezione Postman crea **30 prenotazioni** per i prossimi **6 lunedì** utilizzando i **5 customer esistenti** già creati nel sistema Greedys.

## Struttura delle Prenotazioni

### Customer Utilizzati (Esistenti)
1. **Marco Rossi** - marco.rossi@example.com
2. **Giulia Bianchi** - giulia.bianchi@example.com  
3. **Andrea Verdi** - andrea.verdi@example.com
4. **Francesca Neri** - francesca.neri@example.com
5. **Lorenzo Ferrari** - lorenzo.ferrari@example.com

Password comune: `CustomerPass123!`

### Distribuzione Prenotazioni
- **6 settimane** consecutive di lunedì
- **5 prenotazioni per settimana** (una per customer)
- **30 prenotazioni totali**
- Date calcolate automaticamente dal prossimo lunedì

### Variazioni Realistiche
- **PAX**: da 2 a 6 persone (variabile)
- **Note**: diversificate per tipo di evento
- **Slot**: distribuzione automatica tra gli slot disponibili

## Come Utilizzare

### 1. Configurazione Variabili
La collezione è già configurata con:
```
baseUrl: https://api.greedys.it
restaurantId: 3
```

### 2. Esecuzione Step-by-Step

#### Step 1: Login Customer 🔐
Esegui tutti i 5 login in sequenza per autenticare i customer esistenti.

#### Step 2: Get Monday Slots 📅
Recupera automaticamente tutti gli slot disponibili per il lunedì dal ristorante.

#### Step 3: Create Reservations 🍽️
Crea tutte le 30 prenotazioni automaticamente.

### 3. Esecuzione Automatica
Puoi eseguire l'intera collezione con Postman Runner per:
- Autenticare tutti i customer
- Recuperare gli slot
- Creare tutte le 30 prenotazioni in sequenza

## Output Atteso

### Console Logs
```
✅ Customer 1 (Marco Rossi) logged in successfully
✅ Customer 2 (Giulia Bianchi) logged in successfully
...
✅ Monday slots found: 4
  1. Pranzo: 12:00-14:30 (ID: 123)
  2. Cena: 19:00-21:30 (ID: 124)
...
Prenotazione per Marco Rossi - Slot: 12:00-14:30
Prenotazione per Giulia Bianchi - Slot: 19:00-21:30
...
```

### Date Generate (Esempio)
- 2025-11-10 (Lunedì Week 1)
- 2025-11-17 (Lunedì Week 2)
- 2025-11-24 (Lunedì Week 3)
- 2025-12-01 (Lunedì Week 4)
- 2025-12-08 (Lunedì Week 5)
- 2025-12-15 (Lunedì Week 6)

## Note Tecniche

### Gestione Slot Dinamica
- Gli slot vengono recuperati dinamicamente dal ristorante
- Distribuzione automatica delle prenotazioni tra slot disponibili
- Fallback intelligente se alcuni slot non sono disponibili

### Gestione Date
- Le date vengono calcolate automaticamente dal giorno corrente
- Sistema robusto che gestisce weekends e festività
- Formato date: `YYYY-MM-DD`

### Error Handling
- Verifica login success per ogni customer
- Controllo disponibilità slot prima della prenotazione
- Log dettagliati per troubleshooting

## File Generati

- `30-Monday-Customer-Reservations-Complete.json` - Collezione Postman completa
- `generate-30-reservations.py` - Script Python per la generazione

## Prerequisiti

I 5 customer devono essere già presenti e abilitati nel sistema. 
Utilizzare prima la collezione `Customer-Reservation-Complete-Flow.json` per crearli se necessario.

## Troubleshooting

### Errori Comuni
1. **Customer non autenticati**: Verificare che i customer esistano e siano abilitati
2. **Slot non disponibili**: Controllare che il ristorante abbia slot configurati per il lunedì
3. **Date passate**: Lo script calcola automaticamente date future

### Verifica Sistema
Prima di eseguire, verificare che:
- Il ristorante ID 3 esista
- I customer siano abilitati
- Ci siano slot configurati per il lunedì