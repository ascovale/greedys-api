# 🎯 API Response Standards

## Overview
Questo documento descrive il nuovo standard per le risposte dell'API Greedys per garantire consistenza e migliore esperienza per i client.

## Struttura Standard delle Risposte

### 📋 Formato Base
Tutte le risposte API seguono questa struttura:

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { /* actual response data */ },
  "timestamp": "2025-07-23T10:30:00Z",
  "metadata": { /* optional pagination/additional info */ },
  "error": null
}
```

### ✅ Risposta di Successo
```json
{
  "success": true,
  "message": "Reservation created successfully", 
  "data": {
    "id": 123,
    "pax": 4,
    "date": "2025-07-25",
    "status": "ACCEPTED"
  },
  "timestamp": "2025-07-23T10:30:00Z"
}
```

### ❌ Risposta di Errore
```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "timestamp": "2025-07-23T10:30:00Z",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": [
      {
        "field": "pax",
        "message": "must be greater than 0",
        "rejectedValue": -1
      }
    ]
  }
}
```

### 📄 Risposta Paginata
```json
{
  "success": true,
  "message": "Page 1 of 5 (100 total items)",
  "data": {
    "content": [ /* array of items */ ],
    "pageable": { /* pagination info */ },
    "totalElements": 100,
    "totalPages": 5
  },
  "metadata": {
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "first": true,
    "last": false
  },
  "timestamp": "2025-07-23T10:30:00Z"
}
```

## 🔧 Come Implementare

### 1. Estendere BaseController
```java
@RestController
public class MyController extends BaseController {
    
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<Item>>> getItems() {
        return execute("fetch items", () -> {
            List<Item> items = itemService.findAll();
            return items;
        });
    }
}
```

### 2. Metodi Helper Disponibili

| Metodo | Descrizione | Codice HTTP |
|--------|-------------|-------------|
| `ok(data)` | Successo con dati | 200 |
| `ok(data, message)` | Successo con messaggio custom | 200 |
| `okPaginated(page)` | Successo con paginazione | 200 |
| `created(data, message)` | Risorsa creata | 201 |
| `badRequest(message)` | Richiesta non valida | 400 |
| `notFound(message)` | Risorsa non trovata | 404 |
| `conflict(message)` | Conflitto | 409 |
| `internalServerError(message)` | Errore interno | 500 |

### 3. Gestione Errori Automatica
```java
@GetMapping("/reservation/{id}")
public ResponseEntity<ApiResponse<ReservationDTO>> getReservation(@PathVariable Long id) {
    return execute("fetch reservation", () -> {
        // Se il service lancia NoSuchElementException -> 404 automatico
        // Se il service lancia IllegalArgumentException -> 400 automatico
        return reservationService.findById(id);
    });
}
```

## 📈 Vantaggi

### ✅ Per i Client
- **Consistenza**: Stesso formato per tutte le API
- **Prevedibilità**: Gestione errori standardizzata
- **Metadata**: Informazioni aggiuntive come paginazione
- **Debugging**: Timestamp e codici errore chiari

### ✅ Per gli Sviluppatori
- **Meno Codice**: BaseController riduce boilerplate
- **Standardizzazione**: Automatica gestione errori
- **Manutenibilità**: Cambiamenti centralizzati
- **Testing**: Formato uniforme per i test

## 🔄 Migrazione

### Step 1: Aggiornare Controller Esistenti
```java
// PRIMA
@GetMapping("/reservations")
public Collection<ReservationDTO> getReservations() {
    return reservationService.findAll();
}

// DOPO  
@GetMapping("/reservations")
public ResponseEntity<ApiResponse<Collection<ReservationDTO>>> getReservations() {
    return ok(reservationService.findAll(), "Reservations retrieved successfully");
}
```

### Step 2: Aggiornare Client/Frontend
```javascript
// PRIMA
const reservations = await response.json();

// DOPO
const apiResponse = await response.json();
if (apiResponse.success) {
    const reservations = apiResponse.data;
    console.log(apiResponse.message);
} else {
    console.error(apiResponse.error.message);
}
```

## 🎯 Esempi Completi

### Controller Base
```java
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController extends BaseController {

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationDTO>> create(@RequestBody CreateReservationDTO dto) {
        return execute("create reservation", "Reservation created successfully", () -> {
            return reservationService.create(dto);
        });
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationDTO>> getById(@PathVariable Long id) {
        return execute("fetch reservation", () -> {
            return reservationService.findById(id); // throws NoSuchElementException -> auto 404
        });
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReservationDTO>>> getAll(Pageable pageable) {
        try {
            Page<ReservationDTO> reservations = reservationService.findAll(pageable);
            return okPaginated(reservations);
        } catch (Exception e) {
            return handleException(e, "fetch reservations");
        }
    }
}
```

## 🚀 Benefici Immediati

1. **API più Professional**: Formato enterprise-grade
2. **Error Handling Robusto**: Gestione automatica eccezioni
3. **Client Development Semplificato**: Parsing uniforme
4. **Monitoring Migliorato**: Logging e metriche consistenti
5. **Documentazione Auto-generata**: Swagger con formato standard

## 📝 Best Practices

- ✅ Usare sempre `execute()` per operazioni che possono fallire
- ✅ Fornire messaggi di successo descrittivi
- ✅ Usare `okPaginated()` per liste grandi
- ✅ Loggare operazioni importanti nel controller
- ✅ Mantenere i controller sottili (logica nei service)

Questo nuovo standard renderà l'API più robusta, consistente e facile da usare per tutti i client!
