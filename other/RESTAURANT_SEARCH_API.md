# Restaurant Search API

Questa documentazione descrive le nuove API per la ricerca di ristoranti utilizzando Google Places API, simili alla ricerca di Google Maps.

## Endpoints Disponibili

### 1. Ricerca Singolo Ristorante
**GET** `/api/restaurants/search/single`

Restituisce il primo risultato di ricerca.

**Parametri:**
- `name` (required): Nome del ristorante
- `address` (optional): Indirizzo o località

**Esempio:**
```bash
GET /api/restaurants/search/single?name=Pizza%20Hut&address=Milano
```

### 2. Ricerca Multipla Ristoranti
**GET** `/api/restaurants/search/multiple`

Restituisce una lista di ristoranti trovati.

**Parametri:**
- `name` (required): Nome del ristorante
- `address` (optional): Indirizzo o località

**Esempio:**
```bash
GET /api/restaurants/search/multiple?name=Pizzeria&address=Roma
```

### 3. Ricerca Avanzata
**GET** `/api/restaurants/search/advanced`

Ricerca avanzata con opzioni di filtro e ordinamento.

**Parametri:**
- `query` (required): Query di ricerca (nome ristorante, tipo di cucina, etc.)
- `location` (optional): Località (indirizzo, città, coordinate)
- `maxResults` (optional, default=10): Numero massimo di risultati (max 20)

**Esempi:**
```bash
GET /api/restaurants/search/advanced?query=sushi&location=Milano&maxResults=5
GET /api/restaurants/search/advanced?query=ristorante%20italiano&location=Via%20Roma%20Milano
```

### 4. Ricerca per Categoria
**GET** `/api/restaurants/search/category`

Cerca ristoranti per categoria specifica.

**Parametri:**
- `category` (required): Categoria del ristorante (pizza, sushi, italian, etc.)
- `location` (optional): Località
- `maxResults` (optional, default=10): Numero massimo di risultati

**Esempi:**
```bash
GET /api/restaurants/search/category?category=pizza&location=Milano
GET /api/restaurants/search/category?category=sushi&location=Roma&maxResults=15
```

### 5. Ricerca Nelle Vicinanze
**GET** `/api/restaurants/search/nearby`

Cerca ristoranti vicino a coordinate specifiche.

**Parametri:**
- `latitude` (required): Latitudine
- `longitude` (required): Longitudine  
- `radiusMeters` (optional, default=5000): Raggio di ricerca in metri (max 50000)
- `maxResults` (optional, default=10): Numero massimo di risultati

**Esempio:**
```bash
GET /api/restaurants/search/nearby?latitude=45.4642&longitude=9.1900&radiusMeters=2000&maxResults=10
```

### 6. Dettagli Ristorante
**GET** `/api/restaurants/search/details/{placeId}`

Ottiene informazioni dettagliate su un ristorante specifico tramite Place ID.

**Parametri:**
- `placeId` (path): Google Places ID del ristorante

**Esempio:**
```bash
GET /api/restaurants/search/details/ChIJN1t_tDeuEmsRUsoyG83frY4
```

## Struttura Risposta

### RestaurantSearchResult (ricerca singola)
```json
{
  "found": true,
  "message": "Restaurant found",
  "restaurant": {
    "placeId": "ChIJN1t_tDeuEmsRUsoyG83frY4",
    "name": "Pizza Hut",
    "address": "Via Roma 123, Milano MI, Italia",
    "phoneNumber": "+39 02 1234567",
    "website": "https://www.pizzahut.it",
    "rating": 4.2,
    "priceLevel": 2,
    "types": ["restaurant", "food", "establishment"],
    "photos": [
      "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=...",
      "..."
    ]
  }
}
```

### MultipleRestaurantSearchResult (ricerca multipla)
```json
{
  "found": true,
  "message": "Found 5 restaurants",
  "restaurants": [
    {
      "placeId": "ChIJN1t_tDeuEmsRUsoyG83frY4",
      "name": "Pizza Hut",
      "address": "Via Roma 123, Milano MI, Italia",
      "phoneNumber": "+39 02 1234567",
      "website": "https://www.pizzahut.it",
      "rating": 4.2,
      "priceLevel": 2,
      "types": ["restaurant", "food", "establishment"],
      "photos": ["url1", "url2"]
    },
    {
      "placeId": "ChIJN1t_tDeuEmsRUsoyG83frY5",
      "name": "Domino's Pizza",
      "address": "Via Milano 456, Milano MI, Italia",
      "phoneNumber": "+39 02 7654321",
      "website": "https://www.dominos.it",
      "rating": 3.8,
      "priceLevel": 1,
      "types": ["restaurant", "meal_delivery", "establishment"],
      "photos": ["url3", "url4"]
    }
  ]
}
```

## Caratteristiche Principali

### 🔍 **Ricerca Intelligente**
- Supporta ricerca per nome, tipo di cucina, categoria
- Filtro automatico per risultati di tipo ristorante
- Gestione intelligente delle query di ricerca

### 📍 **Ricerca Geolocalizzata**
- Ricerca per indirizzo o località
- Ricerca per coordinate GPS
- Supporto raggio di ricerca personalizzabile

### 📊 **Ordinamento e Filtri**
- Ordinamento per rating (dal più alto al più basso)
- Ordinamento secondario per nome
- Filtraggio automatico per includere solo ristoranti validi

### 🏷️ **Informazioni Dettagliate**
- Place ID di Google per identificazione univoca
- Informazioni di contatto complete
- Rating e livello prezzo
- Tipologie di ristorante
- Foto del ristorante (fino a 5)

### ⚡ **Performance e Limiti**
- Massimo 20 risultati per ricerca
- Timeout gestiti automaticamente
- Caching delle richieste Google Places API
- Gestione errori robusta

## Codici di Risposta HTTP

- `200 OK`: Ricerca completata con successo
- `404 Not Found`: Nessun ristorante trovato
- `400 Bad Request`: Parametri non validi
- `500 Internal Server Error`: Errore del server

## Esempi di Utilizzo Avanzato

### Cerca pizzerie a Milano
```bash
curl "http://localhost:8080/api/restaurants/search/category?category=pizza&location=Milano&maxResults=10"
```

### Cerca ristoranti giapponesi nelle vicinanze
```bash
curl "http://localhost:8080/api/restaurants/search/advanced?query=sushi%20giapponese&location=Milano%20centro&maxResults=15"
```

### Cerca ristoranti entro 1km
```bash
curl "http://localhost:8080/api/restaurants/search/nearby?latitude=45.4642&longitude=9.1900&radiusMeters=1000&maxResults=20"
```

## Note per gli Sviluppatori

1. **Rate Limiting**: Google Places API ha limiti di quota. Considera l'implementazione di caching.

2. **Autenticazione**: Assicurati che la chiave API Google Maps sia configurata correttamente in `application.properties`.

3. **Error Handling**: Tutti gli endpoint gestiscono automaticamente gli errori e restituiscono messaggi informativi.

4. **Performance**: Per prestazioni migliori, utilizza parametri specifici e limita il numero di risultati.

5. **Espansioni Future**: L'architettura supporta facilmente l'aggiunta di nuovi filtri e criteri di ricerca.
