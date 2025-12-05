# 📸 Sistema Galleria Immagini Ristorante

## Panoramica

Il sistema di gestione delle immagini del ristorante permette di:
- Caricare e gestire il **logo** del ristorante
- Caricare e gestire l'**immagine di copertina**
- Gestire una **galleria di immagini** (fino a 20 immagini)
- Ridimensionamento automatico e compressione ottimizzata
- Generazione automatica di thumbnail

---

## 📐 Specifiche Immagini

### Logo
| Parametro | Valore |
|-----------|--------|
| Dimensione massima | 400 x 400 pixel |
| Aspect ratio | 1:1 (quadrato) |
| Singolo per ristorante | ✅ (sostituisce il precedente) |

### Copertina (Cover)
| Parametro | Valore |
|-----------|--------|
| Dimensione massima | 1920 x 600 pixel |
| Aspect ratio consigliato | 3.2:1 (panoramico) |
| Singola per ristorante | ✅ (sostituisce la precedente) |

### Galleria
| Parametro | Valore |
|-----------|--------|
| Dimensione massima | 1920 x 1080 pixel |
| Massimo immagini | 20 per ristorante |
| Thumbnail | 300 x 200 pixel |

### Comuni a tutti i tipi
| Parametro | Valore |
|-----------|--------|
| Formati supportati | JPEG, PNG, WebP, GIF |
| Dimensione massima file | 10 MB |
| Compressione JPEG | 85% qualità |

---

## 🔗 API Endpoints

### Base URL
```
/restaurant/images
```

### Upload Logo
```http
POST /restaurant/images/logo
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [binary]
```

### Upload Copertina
```http
POST /restaurant/images/cover
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [binary]
title: string (optional)
altText: string (optional)
```

### Upload Immagine Galleria
```http
POST /restaurant/images/gallery
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [binary]
title: string (optional)
altText: string (optional)
```

### Ottieni Logo
```http
GET /restaurant/images/logo
Authorization: Bearer {token}
```

### Ottieni Copertina
```http
GET /restaurant/images/cover
Authorization: Bearer {token}
```

### Ottieni Galleria
```http
GET /restaurant/images/gallery
Authorization: Bearer {token}
```

### Ottieni Tutte le Immagini
```http
GET /restaurant/images
Authorization: Bearer {token}
```

### Aggiorna Metadati Immagine
```http
PUT /restaurant/images/{imageId}
Authorization: Bearer {token}

title: string (optional)
altText: string (optional)
displayOrder: integer (optional)
```

### Imposta Immagine Featured
```http
PUT /restaurant/images/{imageId}/featured
Authorization: Bearer {token}
```

### Riordina Galleria
```http
PUT /restaurant/images/gallery/reorder
Authorization: Bearer {token}

imageIds: [1, 3, 2, 5, 4]
```

### Elimina Immagine (Soft Delete)
```http
DELETE /restaurant/images/{imageId}
Authorization: Bearer {token}
```

### Elimina Immagine (Permanente)
```http
DELETE /restaurant/images/{imageId}/permanent
Authorization: Bearer {token}
```

### Endpoint Pubblici (No Auth)
```http
GET /restaurant/images/public/{restaurantId}
GET /restaurant/images/public/{restaurantId}/gallery
GET /restaurant/images/public/{restaurantId}/logo
GET /restaurant/images/public/{restaurantId}/cover
GET /restaurant/images/file/{restaurantId}/{filename}
```

---

## 📊 Diagramma Sequenza - Upload Immagine

```
┌──────────┐     ┌────────────────────┐     ┌────────────────────┐     ┌─────────────┐
│  Client  │     │ RestaurantImage    │     │ RestaurantImage    │     │  FileSystem │
│  (App)   │     │   Controller       │     │     Service        │     │             │
└────┬─────┘     └─────────┬──────────┘     └─────────┬──────────┘     └──────┬──────┘
     │                     │                          │                       │
     │ 1. POST /images/gallery                        │                       │
     │    [multipart file]                            │                       │
     │────────────────────>│                          │                       │
     │                     │                          │                       │
     │                     │ 2. uploadGalleryImage()  │                       │
     │                     │─────────────────────────>│                       │
     │                     │                          │                       │
     │                     │                          │ 3. validateFile()     │
     │                     │                          │───────────┐           │
     │                     │                          │           │           │
     │                     │                          │<──────────┘           │
     │                     │                          │                       │
     │                     │                          │ 4. checkGalleryLimit()│
     │                     │                          │───────────┐           │
     │                     │                          │           │           │
     │                     │                          │<──────────┘           │
     │                     │                          │                       │
     │                     │                          │ 5. resizeImage()      │
     │                     │                          │───────────┐           │
     │                     │                          │           │           │
     │                     │                          │<──────────┘           │
     │                     │                          │                       │
     │                     │                          │ 6. compressImage()    │
     │                     │                          │───────────┐           │
     │                     │                          │           │           │
     │                     │                          │<──────────┘           │
     │                     │                          │                       │
     │                     │                          │ 7. Save main image    │
     │                     │                          │──────────────────────>│
     │                     │                          │                       │
     │                     │                          │ 8. Generate thumbnail │
     │                     │                          │───────────┐           │
     │                     │                          │           │           │
     │                     │                          │<──────────┘           │
     │                     │                          │                       │
     │                     │                          │ 9. Save thumbnail     │
     │                     │                          │──────────────────────>│
     │                     │                          │                       │
     │                     │                          │ 10. Save to DB        │
     │                     │                          │───────────┐           │
     │                     │                          │           │           │
     │                     │                          │<──────────┘           │
     │                     │                          │                       │
     │                     │ 11. ImageUploadResponse  │                       │
     │                     │<─────────────────────────│                       │
     │                     │                          │                       │
     │ 12. 201 Created     │                          │                       │
     │    {id, paths, ...} │                          │                       │
     │<────────────────────│                          │                       │
     │                     │                          │                       │
```

---

## 📁 Struttura Storage

```
uploads/
└── restaurants/
    └── {restaurantId}/
        ├── abc123.jpg           # Immagine principale
        ├── abc123_thumb.jpg     # Thumbnail
        ├── abc123_original.jpg  # Originale (se > max size)
        ├── def456.jpg
        ├── def456_thumb.jpg
        └── ...
```

---

## 🗄️ Schema Database

### Tabella: `restaurant_gallery_image`

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGINT | PK auto-increment |
| restaurant_id | BIGINT | FK → restaurant |
| image_type | VARCHAR(20) | LOGO, COVER, GALLERY |
| original_filename | VARCHAR(255) | Nome file originale |
| stored_filename | VARCHAR(255) | Nome file salvato (UUID) |
| mime_type | VARCHAR(50) | es: image/jpeg |
| file_size | BIGINT | Dimensione in bytes |
| width | INT | Larghezza pixel |
| height | INT | Altezza pixel |
| file_path | VARCHAR(500) | Path relativo immagine |
| thumbnail_path | VARCHAR(500) | Path relativo thumbnail |
| original_path | VARCHAR(500) | Path originale (nullable) |
| title | VARCHAR(255) | Titolo/descrizione |
| alt_text | VARCHAR(255) | Testo alternativo |
| display_order | INT | Ordine visualizzazione |
| is_featured | BOOLEAN | Flag immagine principale |
| is_active | BOOLEAN | Flag soft delete |
| created_at | TIMESTAMP | Data creazione |
| updated_at | TIMESTAMP | Data modifica |
| created_by | BIGINT | ID utente creatore |

### Indici
- `idx_gallery_restaurant` su `restaurant_id`
- `idx_gallery_type` su `image_type`
- `idx_gallery_order` su `display_order`

---

## 📦 DTO Responses

### ImageUploadResponse
```json
{
  "id": 1,
  "imageType": "GALLERY",
  "filePath": "123/abc-123.jpg",
  "thumbnailPath": "123/abc-123_thumb.jpg",
  "width": 1920,
  "height": 1080,
  "fileSize": 245678,
  "fileSizeReadable": "240.0 KB",
  "message": "Immagine caricata con successo"
}
```

### RestaurantGalleryImageDTO
```json
{
  "id": 1,
  "restaurantId": 123,
  "imageType": "GALLERY",
  "originalFilename": "vista_panoramica.jpg",
  "mimeType": "image/jpeg",
  "fileSize": 245678,
  "fileSizeReadable": "240.0 KB",
  "width": 1920,
  "height": 1080,
  "aspectRatio": 1.78,
  "filePath": "123/abc-123.jpg",
  "thumbnailPath": "123/abc-123_thumb.jpg",
  "title": "Vista panoramica",
  "altText": "Vista del ristorante",
  "displayOrder": 1,
  "isFeatured": false,
  "createdAt": "2025-12-01T10:30:00Z"
}
```

---

## ⚙️ Configurazione

### application.properties

```properties
# Directory di upload
app.image.upload-dir=uploads/restaurants

# Dimensioni massime immagini (pixel)
app.image.max-width=1920
app.image.max-height=1080

# Dimensioni thumbnail (pixel)
app.image.thumbnail-width=300
app.image.thumbnail-height=200

# Dimensioni logo (pixel)
app.image.logo-width=400
app.image.logo-height=400

# Dimensioni copertina (pixel)
app.image.cover-width=1920
app.image.cover-height=600

# Qualità compressione JPEG (0.0 - 1.0)
app.image.compression-quality=0.85

# Dimensione massima file upload (bytes) - 10MB
app.image.max-file-size=10485760

# Numero massimo immagini galleria
app.image.max-gallery-images=20
```

---

## 🔒 Sicurezza

- Gli endpoint protetti richiedono autenticazione Bearer JWT
- Gli endpoint pubblici (`/public/`) non richiedono autenticazione
- Il controller verifica che l'immagine appartenga al ristorante dell'utente
- Validazione MIME type e estensione file
- Soft delete di default per preservare le immagini

---

## 📂 File Creati

```
greedys_api/src/main/java/com/application/
├── common/
│   ├── exception/
│   │   ├── InvalidImageException.java
│   │   └── ImageProcessingException.java
│   └── persistence/
│       ├── model/
│       │   └── RestaurantGalleryImage.java
│       └── dao/
│           └── RestaurantGalleryImageDAO.java
└── restaurant/
    ├── controller/restaurant/
    │   └── RestaurantImageController.java
    └── service/image/
        ├── RestaurantImageService.java
        └── dto/
            ├── RestaurantGalleryImageDTO.java
            └── ImageUploadResponse.java
```

---

## 🧪 Esempi cURL

### Upload Logo
```bash
curl -X POST http://localhost:8080/restaurant/images/logo \
  -H "Authorization: Bearer {token}" \
  -F "file=@logo.png"
```

### Upload Immagine Galleria
```bash
curl -X POST http://localhost:8080/restaurant/images/gallery \
  -H "Authorization: Bearer {token}" \
  -F "file=@foto_ristorante.jpg" \
  -F "title=Sala principale" \
  -F "altText=Vista della sala principale del ristorante"
```

### Ottieni Galleria Pubblica
```bash
curl http://localhost:8080/restaurant/images/public/123/gallery
```

### Download Immagine
```bash
curl http://localhost:8080/restaurant/images/file/123/abc-123.jpg -o image.jpg
```
