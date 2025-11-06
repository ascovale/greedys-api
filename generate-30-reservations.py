#!/usr/bin/env python3
"""
Genera 30 prenotazioni per i prossimi 6 lunedì distribuiti tra 5 customer
"""
import json
from datetime import datetime, timedelta

# Template per una singola prenotazione
reservation_template = {
    "name": "",
    "request": {
        "method": "POST",
        "header": [
            {
                "key": "Content-Type",
                "value": "application/json"
            },
            {
                "key": "Authorization",
                "value": ""
            }
        ],
        "body": {
            "mode": "raw",
            "raw": ""
        },
        "url": {
            "raw": "{{baseUrl}}/customer/reservation",
            "host": ["{{baseUrl}}"],
            "path": ["customer", "reservation"]
        }
    },
    "event": [
        {
            "listen": "prerequest",
            "script": {
                "exec": []
            }
        }
    ]
}

# Dati dei customer esistenti
customers = [
    {"name": "Marco Rossi", "token": "customer1_token", "email": "marco.rossi@example.com"},
    {"name": "Giulia Bianchi", "token": "customer2_token", "email": "giulia.bianchi@example.com"},
    {"name": "Andrea Verdi", "token": "customer3_token", "email": "andrea.verdi@example.com"},
    {"name": "Francesca Neri", "token": "customer4_token", "email": "francesca.neri@example.com"},
    {"name": "Lorenzo Ferrari", "token": "customer5_token", "email": "lorenzo.ferrari@example.com"}
]

# Note varie per le prenotazioni
notes_variations = [
    "Cena romantica",
    "Pranzo di lavoro", 
    "Cena con amici",
    "Compleanno famiglia",
    "Appuntamento importante",
    "Anniversario",
    "Cena informale",
    "Pranzo domenicale",
    "Festeggiamento",
    "Incontro clienti",
    "Cena di gruppo",
    "Tavolo riservato"
]

# PAX variabili
pax_options = [2, 2, 3, 4, 2, 5, 6, 3, 4, 2, 3, 4]

def calculate_next_mondays(count=6):
    """Calcola le prossime 6 date del lunedì"""
    today = datetime.now()
    days_until_monday = (7 - today.weekday()) % 7
    if days_until_monday == 0:  # Se oggi è lunedì
        days_until_monday = 7
    
    next_monday = today + timedelta(days=days_until_monday)
    
    mondays = []
    for i in range(count):
        monday = next_monday + timedelta(weeks=i)
        mondays.append(monday.strftime("%Y-%m-%d"))
    
    return mondays

def generate_reservations():
    """Genera tutte le 30 prenotazioni"""
    reservations = []
    mondays = calculate_next_mondays(6)
    
    reservation_count = 0
    
    for week_num, monday_date in enumerate(mondays):
        # 5 prenotazioni per settimana per 6 settimane = 30 totali
        for customer_index in range(5):
            if reservation_count >= 30:
                break
                
            customer = customers[customer_index]
            
            # Crea prenotazione
            reservation = json.loads(json.dumps(reservation_template))
            
            reservation["name"] = f"Reservation {reservation_count + 1} - {customer['name']} (Week {week_num + 1})"
            reservation["request"]["header"][1]["value"] = f"Bearer {{{{{customer['token']}}}}}"
            
            # Body della prenotazione
            pax = pax_options[reservation_count % len(pax_options)]
            note = f"{notes_variations[reservation_count % len(notes_variations)]} - {customer['name']}"
            
            body_data = {
                "restaurantId": "{{restaurantId}}",
                "pax": pax,
                "note": note,
                "slotId": f"{{{{slot_id_{(reservation_count % 3) + 1}}}}}",
                "date": monday_date
            }
            
            reservation["request"]["body"]["raw"] = json.dumps(body_data, indent=2)
            
            # Script pre-request per gestire slot dinamici
            prerequest_script = [
                f"// Prenotazione {reservation_count + 1} per {monday_date}",
                "const mondaySlots = JSON.parse(pm.collectionVariables.get('monday_slots') || '[]');",
                "if (mondaySlots.length > 0) {",
                f"    const slotIndex = {reservation_count % 3};",
                "    const slot = mondaySlots[slotIndex % mondaySlots.length];",
                f"    pm.request.body.raw = pm.request.body.raw.replace('\"{{{{slot_id_{(reservation_count % 3) + 1}}}}}\"', `\"${{slot.id}}\"`);",
                "    console.log(`Prenotazione per ${customer['name']} - Slot: ${slot.start}-${slot.end}`);",
                "} else {",
                "    console.log('❌ No Monday slots available');",
                "}"
            ]
            
            reservation["event"][0]["script"]["exec"] = prerequest_script
            
            reservations.append(reservation)
            reservation_count += 1
            
            if reservation_count >= 30:
                break
    
    return reservations

def main():
    # Leggi il file base
    with open('/home/valentino/workspace/greedysgroup/greedys_api/test-postman/30-Monday-Customer-Reservations-Complete.json', 'r') as f:
        collection = json.load(f)
    
    # Genera tutte le prenotazioni
    reservations = generate_reservations()
    
    # Aggiungi le prenotazioni alla collezione
    for item in collection["item"]:
        if item["name"] == "🍽️ Step 3: Create 30 Monday Reservations":
            item["item"] = reservations
            break
    
    # Salva il file aggiornato
    with open('/home/valentino/workspace/greedysgroup/greedys_api/test-postman/30-Monday-Customer-Reservations-Complete.json', 'w') as f:
        json.dump(collection, f, indent=2, ensure_ascii=False)
    
    print(f"✅ Generata collezione con {len(reservations)} prenotazioni")
    print(f"📅 Date lunedì: {', '.join(calculate_next_mondays(6))}")
    print(f"👥 Customer: {', '.join([c['name'] for c in customers])}")

if __name__ == "__main__":
    main()