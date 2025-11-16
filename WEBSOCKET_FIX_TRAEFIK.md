# WebSocket Fix - Traefik Configuration

## ❌ Problema Trovato

L'app riceveva errore:
```
GET wss://api.greedys.it/ws
NS_ERROR_WEBSOCKET_CONNECTION_REFUSED
HTTP/1.1 400 Bad Request
```

**Causa**: Traefik non aveva configurazione esplicita per supportare WebSocket (HTTP upgrade)

## ✅ Soluzione Applicata

### 1. traefik/dynamic.yml - Aggiunto middleware WebSocket

```yaml
middlewares:
  # ⭐ WebSocket Middleware - Permetti upgrade HTTP→WebSocket
  websocket:
    headers:
      customRequestHeaders:
        Upgrade: "websocket"
        Connection: "Upgrade"
        Sec-WebSocket-Version: "13"
```

### 2. docker-compose.prod.yml - Aggiunto supporto HTTP upgrade

```yaml
labels:
  # ⭐ WebSocket Support - Abilita HTTP upgrade per WebSocket
  - "traefik.http.services.greedys-api.loadbalancer.passhostheader=true"
  # Aggiunto middleware websocket
  - "traefik.http.routers.greedys-api.middlewares=cors,security,websocket"
```

## 📋 Cosa Fare Ora

### Opzione 1: Deploy Immediato (Produzione)
```bash
# Stai su un server con Docker Swarm
docker stack deploy -c docker-compose.prod.yml greedys

# Traefik leggerà la nuova configurazione in ~5 secondi
# L'app Flutter dovrebbe connettersi a wss://api.greedys.it/ws ✅
```

### Opzione 2: Test Locale
```bash
# Prova la configurazione in locale
docker-compose -f docker-compose.prod.yml up -d

# Testa la WebSocket
curl -v --http1.1 \
  -H "Upgrade: websocket" \
  -H "Connection: Upgrade" \
  -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
  -H "Sec-WebSocket-Version: 13" \
  http://localhost:8080/ws

# Dovrebbe rispondere con HTTP 101 Switching Protocols
```

## 🔧 Cosa è Stato Modificato

| File | Cambiamento |
|------|-----------|
| `traefik/dynamic.yml` | Aggiunto middleware `websocket` con header corretti |
| `docker-compose.prod.yml` | Aggiunto `passhostheader=true` e middleware `websocket` |

## 📚 Tecnico

**Cosa fa il fix:**

1. **`passhostheader=true`**: Traefik passa l'header `Host` al backend intatto (importante per Spring WebSocket)
2. **Middleware `websocket`**: Aggiunge gli header necessari per HTTP upgrade:
   - `Upgrade: websocket` - Dice al server di upgradare la connessione
   - `Connection: Upgrade` - Richiede il cambio di protocollo
   - `Sec-WebSocket-Version: 13` - Specifica la versione WebSocket

3. **Spring Boot** riceve questi header e risponde:
   - `HTTP/1.1 101 Switching Protocols` ✅
   - La connessione diventa un tunnel WebSocket

## ✅ Come Verificare il Fix

Dopo il deploy, l'app dovrebbe ricevere questo flusso:

```
Firefox   →  Traefik:443 (wss://)  →  Traefik:8080 (ws://)  →  Spring Boot:8080 (/ws)
                ↓                           ↓                        ↓
         101 Switching Protocols   101 Switching Protocols    101 Switching Protocols
                ↓                           ↓                        ↓
              WebSocket Upgrade
```

L'app Flutter dovrebbe ricevere:
```json
{
  "type": "RESERVATION_CREATED|ACCEPTED|REJECTED",
  "reservation": { ... },
  "timestamp": "2025-11-16T10:30:00Z"
}
```

## 🚀 Next Steps

1. Deploy il fix (`docker stack deploy`)
2. Aspetta 5-10 secondi
3. L'app Flutter fa connessione a `wss://api.greedys.it/ws`
4. Dovrebbe ricevere i messaggi di prenotazione in real-time ✅
