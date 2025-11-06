const fs = require('fs');

// Dati per le prenotazioni
const customers = ['customer1', 'customer2', 'customer3', 'customer4'];
const names = [
    'Mario Rossi', 'Giulia Bianchi', 'Luca Verde', 'Anna Neri',
    'Francesco Romano', 'Elena Conti', 'Andrea Moretti', 'Sofia Greco',
    'Paolo Ferrari', 'Chiara Costa', 'Marco Ricci', 'Valentina Russo',
    'Davide Colombo', 'Federica Bruno', 'Matteo Gallo', 'Alessia Rizzo'
];

const notes = [
    'Tavolo vicino alla finestra per favore',
    'Allergia ai crostacei',
    'Compleanno - serve una torta',
    'Cena di lavoro',
    'Anniversario di matrimonio',
    'Prima volta nel ristorante',
    'Cena romantica',
    null,
    null
];

// Calcola le prossime 8 date di lunedì
function getNextMondays() {
    const mondays = [];
    let date = new Date('2025-11-11'); // Prossimo lunedì
    
    for (let i = 0; i < 8; i++) {
        const monday = new Date(date);
        monday.setDate(date.getDate() + (i * 7));
        mondays.push(monday.toISOString().split('T')[0]);
    }
    
    return mondays;
}

const mondayDates = getNextMondays();

// Genera le 30 prenotazioni
const reservations = [];

for (let i = 0; i < 30; i++) {
    const customerIndex = i % 4;
    const customer = customers[customerIndex];
    const name = names[i % names.length];
    const pax = Math.floor(Math.random() * 5) + 2; // 2-6 persone
    const kids = Math.floor(Math.random() * 3); // 0-2 bambini
    const note = notes[Math.floor(Math.random() * notes.length)];
    const dateIndex = Math.floor(Math.random() * mondayDates.length);
    const reservationDate = mondayDates[dateIndex];
    
    const reservation = {
        "name": `Prenotazione ${i + 1} - ${name} (${customer})`,
        "request": {
            "method": "POST",
            "header": [
                {
                    "key": "Authorization",
                    "value": `Bearer {{${customer}_token}}`
                },
                {
                    "key": "Content-Type",
                    "value": "application/json"
                }
            ],
            "body": {
                "mode": "raw",
                "raw": JSON.stringify({
                    "userName": name,
                    "idSlot": "{{slot1}}", // Useremo slot casuali nel test
                    "pax": pax,
                    "kids": kids,
                    "notes": note,
                    "reservationDay": reservationDate
                }, null, 2)
            },
            "url": {
                "raw": "{{baseUrl}}/customer/reservation/ask",
                "host": ["{{baseUrl}}"],
                "path": ["customer", "reservation", "ask"]
            }
        },
        "event": [
            {
                "listen": "test",
                "script": {
                    "exec": [
                        "if (pm.response.code === 201) {",
                        `    console.log('✅ Prenotazione ${i + 1} creata: ${name}');`,
                        "    const response = pm.response.json();",
                        `    pm.collectionVariables.set('reservation_${i + 1}_id', response.id);`,
                        "} else {",
                        `    console.log('❌ Errore prenotazione ${i + 1}: ' + pm.response.text());`,
                        "}"
                    ]
                }
            }
        ]
    };
    
    reservations.push(reservation);
}

// Crea la collezione completa
const collection = {
    "info": {
        "name": "30 Customer Reservations - Monday Collection",
        "description": "Collezione per creare 30 prenotazioni di customer diversi per i prossimi lunedì",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "variable": [
        {
            "key": "baseUrl",
            "value": "http://api.greedys.it:8080",
            "type": "string"
        },
        {
            "key": "restaurantId",
            "value": "3",
            "type": "string"
        },
        {
            "key": "customer1_token",
            "value": "",
            "type": "string"
        },
        {
            "key": "customer2_token", 
            "value": "",
            "type": "string"
        },
        {
            "key": "customer3_token",
            "value": "",
            "type": "string"
        },
        {
            "key": "customer4_token",
            "value": "",
            "type": "string"
        },
        {
            "key": "slot1",
            "value": "",
            "type": "string"
        },
        {
            "key": "slot2",
            "value": "",
            "type": "string"
        },
        {
            "key": "slot3",
            "value": "",
            "type": "string"
        }
    ],
    "item": [
        {
            "name": "🔐 Setup - Customer Logins",
            "item": [
                {
                    "name": "Login Customer 1",
                    "request": {
                        "method": "POST",
                        "header": [
                            {
                                "key": "Content-Type",
                                "value": "application/json"
                            }
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"email\": \"customer1@test.com\",\n  \"password\": \"password123\"\n}"
                        },
                        "url": {
                            "raw": "{{baseUrl}}/customer/auth/login",
                            "host": ["{{baseUrl}}"],
                            "path": ["customer", "auth", "login"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "exec": [
                                    "if (pm.response.code === 200) {",
                                    "    const response = pm.response.json();",
                                    "    pm.collectionVariables.set('customer1_token', response.accessToken);",
                                    "    console.log('✅ Customer 1 logged in');",
                                    "} else {",
                                    "    console.log('❌ Customer 1 login failed');",
                                    "}"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "Login Customer 2", 
                    "request": {
                        "method": "POST",
                        "header": [
                            {
                                "key": "Content-Type",
                                "value": "application/json"
                            }
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"email\": \"customer2@test.com\",\n  \"password\": \"password123\"\n}"
                        },
                        "url": {
                            "raw": "{{baseUrl}}/customer/auth/login",
                            "host": ["{{baseUrl}}"],
                            "path": ["customer", "auth", "login"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "exec": [
                                    "if (pm.response.code === 200) {",
                                    "    const response = pm.response.json();",
                                    "    pm.collectionVariables.set('customer2_token', response.accessToken);",
                                    "    console.log('✅ Customer 2 logged in');",
                                    "} else {",
                                    "    console.log('❌ Customer 2 login failed');",
                                    "}"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "Login Customer 3",
                    "request": {
                        "method": "POST",
                        "header": [
                            {
                                "key": "Content-Type",
                                "value": "application/json"
                            }
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"email\": \"customer3@test.com\",\n  \"password\": \"password123\"\n}"
                        },
                        "url": {
                            "raw": "{{baseUrl}}/customer/auth/login",
                            "host": ["{{baseUrl}}"],
                            "path": ["customer", "auth", "login"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "exec": [
                                    "if (pm.response.code === 200) {",
                                    "    const response = pm.response.json();",
                                    "    pm.collectionVariables.set('customer3_token', response.accessToken);",
                                    "    console.log('✅ Customer 3 logged in');",
                                    "} else {",
                                    "    console.log('❌ Customer 3 login failed');",
                                    "}"
                                ]
                            }
                        }
                    ]
                },
                {
                    "name": "Login Customer 4",
                    "request": {
                        "method": "POST",
                        "header": [
                            {
                                "key": "Content-Type",
                                "value": "application/json"
                            }
                        ],
                        "body": {
                            "mode": "raw",
                            "raw": "{\n  \"email\": \"customer4@test.com\",\n  \"password\": \"password123\"\n}"
                        },
                        "url": {
                            "raw": "{{baseUrl}}/customer/auth/login",
                            "host": ["{{baseUrl}}"],
                            "path": ["customer", "auth", "login"]
                        }
                    },
                    "event": [
                        {
                            "listen": "test",
                            "script": {
                                "exec": [
                                    "if (pm.response.code === 200) {",
                                    "    const response = pm.response.json();",
                                    "    pm.collectionVariables.set('customer4_token', response.accessToken);",
                                    "    console.log('✅ Customer 4 logged in');",
                                    "} else {",
                                    "    console.log('❌ Customer 4 login failed');",
                                    "}"
                                ]
                            }
                        }
                    ]
                }
            ]
        },
        {
            "name": "📅 Get Monday Slots",
            "request": {
                "method": "GET",
                "header": [],
                "url": {
                    "raw": "{{baseUrl}}/customer/availability/{{restaurantId}}/slots?date=2025-11-11",
                    "host": ["{{baseUrl}}"],
                    "path": ["customer", "availability", "{{restaurantId}}", "slots"],
                    "query": [
                        {
                            "key": "date",
                            "value": "2025-11-11",
                            "description": "Prossimo lunedì"
                        }
                    ]
                }
            },
            "event": [
                {
                    "listen": "test",
                    "script": {
                        "exec": [
                            "if (pm.response.code === 200) {",
                            "    const slots = pm.response.json();",
                            "    console.log('Slots trovati:', slots.length);",
                            "    ",
                            "    // Filtra slot del lunedì e salva i primi 3",
                            "    const mondaySlots = slots.filter(slot => ",
                            "        slot.weekday === 'MONDAY' || slot.day === 'MONDAY'",
                            "    );",
                            "    ",
                            "    if (mondaySlots.length > 0) {",
                            "        pm.collectionVariables.set('slot1', mondaySlots[0].id);",
                            "        console.log('✅ Slot 1 salvato:', mondaySlots[0].id);",
                            "        ",
                            "        if (mondaySlots.length > 1) {",
                            "            pm.collectionVariables.set('slot2', mondaySlots[1].id);",
                            "            console.log('✅ Slot 2 salvato:', mondaySlots[1].id);",
                            "        }",
                            "        ",
                            "        if (mondaySlots.length > 2) {",
                            "            pm.collectionVariables.set('slot3', mondaySlots[2].id);",
                            "            console.log('✅ Slot 3 salvato:', mondaySlots[2].id);",
                            "        }",
                            "        ",
                            "        console.log('🎯 Monday slots disponibili:', mondaySlots.length);",
                            "    } else {",
                            "        console.log('❌ Nessun slot del lunedì trovato');",
                            "    }",
                            "} else {",
                            "    console.log('❌ Errore nel recuperare gli slot');",
                            "}"
                        ]
                    }
                }
            ]
        },
        {
            "name": "🎯 Create 30 Reservations",
            "item": reservations
        }
    ]
};

// Salva il file
console.log('Generando collezione con 30 prenotazioni...');
fs.writeFileSync('./30-Monday-Customer-Reservations-Complete.json', JSON.stringify(collection, null, 2));
console.log('✅ Collezione generata: 30-Monday-Customer-Reservations-Complete.json');
console.log(`📊 Statistiche:
- 30 prenotazioni totali
- 4 customer diversi
- ${mondayDates.length} lunedì diversi
- Date: ${mondayDates.join(', ')}
- Nomi casuali e dati realistici
`);