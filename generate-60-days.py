#!/usr/bin/env python3
"""
Generate a comprehensive Postman collection with 60 days of reservations
cycling through 3 services and 3 people groups
"""

import json
from datetime import datetime, timedelta

# Base collection template
collection = {
    "info": {
        "_postman_id": "bulk-60-days",
        "name": "60-Day Bulk Reservations",
        "description": "Create 60 reservations (tomorrow to 60 days out) cycling through services and people",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "item": []
}

# People rotation
people = [
    {"name": "Marco Rossi", "email": "marco@example.com", "phone": "+39 333 1234567", "pax": 2, "kids": 0},
    {"name": "Giovanni Verdi", "email": "giovanni@example.com", "phone": "+39 333 7654321", "pax": 4, "kids": 2},
    {"name": "Lucia Bianchi", "email": "lucia@example.com", "phone": "+39 333 5555555", "pax": 3, "kids": 1}
]

# Services rotation
services = [
    {"name": "Colazione", "slotId": 202},
    {"name": "Pranzo", "slotId": 203},
    {"name": "Cena", "slotId": 204}
]

# Login step
login_request = {
    "name": "Step 1: Login",
    "event": [
        {
            "listen": "test",
            "script": {
                "exec": [
                    "console.log('\\n🔐 LOGIN');",
                    "if (pm.response.code === 200) {",
                    "    var jsonData = pm.response.json();",
                    "    pm.environment.set('restaurantToken', jsonData.jwt);",
                    "    pm.environment.set('restaurantId', jsonData.user.restaurantId);",
                    "    console.log('✅ Token saved, Restaurant ID:', jsonData.user.restaurantId);",
                    "}"
                ],
                "type": "text/javascript"
            }
        }
    ],
    "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
            "mode": "raw",
            "raw": "{\"username\": \"test@test.it\", \"password\": \"TestPass123!\", \"rememberMe\": true}"
        },
        "url": {
            "raw": "{{baseUrl}}/restaurant/user/auth/login",
            "host": ["{{baseUrl}}"],
            "path": ["restaurant", "user", "auth", "login"]
        }
    }
}

collection["item"].append(login_request)

# Generate 60 reservation requests
today = datetime.now()

for day_num in range(1, 61):
    res_date = today + timedelta(days=day_num)
    date_str = res_date.strftime("%Y-%m-%d")
    
    person_idx = (day_num - 1) % len(people)
    service_idx = (day_num - 1) % len(services)
    
    person = people[person_idx]
    service = services[service_idx]
    
    # Pre-request script to set date
    prereq_script = f"""const d = new Date();
d.setDate(d.getDate() + {day_num});
const s = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
pm.environment.set('resDate', s);"""
    
    # Test script
    test_script = f"""if (pm.response.code === 201 || pm.response.code === 200) {{
  try {{
    var d = pm.response.json();
    if (d.id) console.log('✅ Day {day_num} - ID: ' + d.id + ' ({service["name"]})');
  }} catch(e) {{}}
}} else {{
  console.log('❌ Day {day_num} FAILED - Status: ' + pm.response.code);
}}"""
    
    request_body = {
        "userName": person["name"],
        "userEmail": person["email"],
        "userPhoneNumber": person["phone"],
        "idSlot": service["slotId"],
        "pax": person["pax"],
        "kids": person["kids"],
        "notes": f"Day {day_num} - {service['name']}",
        "reservationDay": "{{resDate}}"
    }
    
    request_item = {
        "name": f"Day {day_num}: {date_str.split('-')[-1]}-{date_str.split('-')[1]} ({service['name']})",
        "event": [
            {
                "listen": "prerequest",
                "script": {
                    "exec": [prereq_script],
                    "type": "text/javascript"
                }
            },
            {
                "listen": "test",
                "script": {
                    "exec": [test_script],
                    "type": "text/javascript"
                }
            }
        ],
        "request": {
            "method": "POST",
            "header": [
                {"key": "Authorization", "value": "Bearer {{restaurantToken}}"},
                {"key": "Content-Type", "value": "application/json"}
            ],
            "body": {
                "mode": "raw",
                "raw": json.dumps(request_body)
            },
            "url": {
                "raw": "{{baseUrl}}/restaurant/reservation/new",
                "host": ["{{baseUrl}}"],
                "path": ["restaurant", "reservation", "new"]
            }
        }
    }
    
    collection["item"].append(request_item)

# Verification step
verification_request = {
    "name": "Step 2: Verify All 60 Reservations",
    "event": [
        {
            "listen": "prerequest",
            "script": {
                "exec": [
                    "const start = new Date();",
                    "start.setDate(start.getDate() - 10);",
                    "const end = new Date();",
                    "end.setDate(end.getDate() + 90);",
                    "const startStr = start.getFullYear() + '-' + String(start.getMonth() + 1).padStart(2, '0') + '-' + String(start.getDate()).padStart(2, '0');",
                    "const endStr = end.getFullYear() + '-' + String(end.getMonth() + 1).padStart(2, '0') + '-' + String(end.getDate()).padStart(2, '0');",
                    "pm.environment.set('queryStart', startStr);",
                    "pm.environment.set('queryEnd', endStr);"
                ],
                "type": "text/javascript"
            }
        },
        {
            "listen": "test",
            "script": {
                "exec": [
                    "console.log('\\n📊 FINAL VERIFICATION');",
                    "if (pm.response.code === 200) {",
                    "    const data = pm.response.json();",
                    "    if (Array.isArray(data)) {",
                    "        console.log('✅ Total reservations:', data.length);",
                    "        const byService = {};",
                    "        let totalPax = 0;",
                    "        data.forEach(r => {",
                    "            totalPax += r.pax || 0;",
                    "            if (r.slot && r.slot.service) {",
                    "                const svc = r.slot.service.name;",
                    "                byService[svc] = (byService[svc] || 0) + 1;",
                    "            }",
                    "        });",
                    "        console.log('👥 Total pax:', totalPax);",
                    "        console.log('📅 By service:', byService);",
                    "    }",
                    "}"
                ],
                "type": "text/javascript"
            }
        }
    ],
    "request": {
        "method": "GET",
        "header": [{"key": "Authorization", "value": "Bearer {{restaurantToken}}"}],
        "url": {
            "raw": "{{baseUrl}}/restaurant/reservation/reservations?start={{queryStart}}&end={{queryEnd}}",
            "host": ["{{baseUrl}}"],
            "path": ["restaurant", "reservation", "reservations"],
            "query": [
                {"key": "start", "value": "{{queryStart}}"},
                {"key": "end", "value": "{{queryEnd}}"}
            ]
        }
    }
}

collection["item"].append(verification_request)

# Write to file
output_file = "/mnt/c/Users/ascol/Projects/greedys_api/test-postman/60-Days-Full-Bulk.json"
with open(output_file, 'w') as f:
    json.dump(collection, f, indent=2)

print(f"✅ Generated {len(collection['item']) - 2} reservation requests (+ login + verification)")
print(f"📝 File saved: {output_file}")
print(f"\n🔄 Rotation pattern:")
print(f"   People: {len(people)} ({', '.join([p['name'] for p in people])})")
print(f"   Services: {len(services)} ({', '.join([s['name'] for s in services])})")
print(f"   Total pax if all created: {sum([p['pax'] for p in people]) * 20}")
