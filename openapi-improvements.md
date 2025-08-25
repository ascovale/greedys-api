# Miglioramenti OpenAPI Suggeriti

## 1. Aggiungere esempi negli schema

```json
"AuthRequestDTO": {
  "type": "object",
  "properties": {
    "username": {
      "type": "string",
      "example": "restaurant@example.com",
      "minLength": 1,
      "maxLength": 100
    },
    "password": {
      "type": "string",
      "example": "SecurePassword123!",
      "minLength": 8
    }
  }
}
```

## 2. Migliorare le descrizioni delle operazioni

Invece di:
```json
"summary": "Create a new reservation"
```

Meglio:
```json
"summary": "Create a new reservation",
"description": "Creates a new restaurant reservation with specified date, time, party size and special requirements. Returns reservation ID and confirmation details."
```

## 3. Status codes specifici per operazione

```json
"responses": {
  "201": {
    "description": "Reservation created successfully",
    "content": {
      "application/json": {
        "schema": {
          "$ref": "#/components/schemas/ResponseWrapper"
        }
      }
    }
  },
  "409": {
    "description": "Conflict - Time slot not available",
    "content": {
      "application/json": {
        "schema": {
          "$ref": "#/components/schemas/ResponseWrapper"
        }
      }
    }
  }
}
```

## 4. Validazioni nei parametri

```json
"parameters": [{
  "name": "reservationId",
  "in": "path",
  "required": true,
  "schema": {
    "type": "integer",
    "format": "int64",
    "minimum": 1
  },
  "example": 12345
}]
```

## 5. Esempi nelle richieste

```json
"requestBody": {
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/RestaurantNewReservationDTO"
      },
      "examples": {
        "dinner_reservation": {
          "summary": "Dinner reservation for 4 people",
          "value": {
            "userName": "Mario Rossi",
            "idSlot": 15,
            "pax": 4,
            "kids": 0,
            "notes": "Anniversary dinner",
            "reservationDay": "2024-12-25",
            "userEmail": "mario.rossi@email.com",
            "userPhoneNumber": "+393331234567"
          }
        }
      }
    }
  }
}
```

## 6. Correggere typo nel tag

```json
// Da:
"name": "Resttaurant Registration"
// A:
"name": "Restaurant Registration"
```

## 7. Aggiungere server environments

```json
"servers": [
  {
    "url": "http://localhost:8080",
    "description": "Development server"
  },
  {
    "url": "https://api.greedys.com",
    "description": "Production server"
  }
]
```

## 8. Migliorare metadata del ResponseWrapper

```json
"ResponseWrapper": {
  "type": "object",
  "properties": {
    "success": {
      "type": "boolean",
      "description": "Indicates if the request was successful",
      "example": true
    },
    "message": {
      "type": "string",
      "description": "Human-readable response message",
      "example": "Reservation created successfully"
    },
    "data": {
      "description": "Response payload data",
      "oneOf": [
        {"$ref": "#/components/schemas/ReservationDTO"},
        {"$ref": "#/components/schemas/ServiceDTO"}
      ]
    }
  }
}
```
