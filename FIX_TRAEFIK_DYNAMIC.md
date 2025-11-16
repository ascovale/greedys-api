# ⚠️ Fix Urgente: Traefik Dynamic Configuration

## Problema
Il file `traefik/dynamic.yml` non è stato aggiornato sul server. Traefik non riesce a leggere la configurazione dinamica.

## Soluzione

### Opzione 1: Redeploy (se hai accesso)

```bash
# Sul server, copia il file aggiornato
cd ~/greedys_api
scp -r traefik/ deployer@46.101.209.92:~/greedys_api/

# Restart Traefik per ricaricare la config
docker service update --force greedys_api_traefik
```

### Opzione 2: Edita direttamente sul server

SSH nel server:
```bash
ssh -i deploy_key deployer@46.101.209.92
cd ~/greedys_api/traefik
```

Edita `dynamic.yml` e aggiungi dopo `http:`:

```yaml
http:
  middlewares:
    # CORS Middleware
    cors:
      headers:
        accessControlAllowMethods:
          - GET
          - POST
          - PUT
          - DELETE
          - OPTIONS
          - PATCH
        accessControlAllowOriginList:
          - "https://greedys.it"
          - "https://www.greedys.it"
          - "https://app.greedys.it"
          - "http://localhost:8081"  # ← AGGIUNGI QUESTA RIGA PER DEV
        accessControlAllowHeaders:
          - "*"
        accessControlExposeHeaders:
          - "Authorization"
          - "Content-Type"
        accessControlAllowCredentials: true
        accessControlMaxAge: 86400
```

Poi restart Traefik:
```bash
docker service update --force greedys_api_traefik
```

### Opzione 3: Push e redeploy

```bash
# Locale
git add traefik/dynamic.yml docker-compose.yml
git commit -m "Fix Traefik WebSocket configuration"
git push origin main

# Aspetta ~2 minuti che la pipeline deploya
```

## File da verificare

Sul server, controlla che:

1. `~/greedys_api/traefik/dynamic.yml` esista e sia aggiornato
2. Traefik l'abbia ricaricato:
   ```bash
   docker service logs greedys_api_traefik | grep -i "dynamic\|config"
   ```

## Cosa dovreste fare

1. Scegli una delle 3 opzioni sopra
2. Verifica che Traefik abbia ricaricato la config
3. Testa:
   ```bash
   curl -v https://api.greedys.it/swagger-ui/index.html
   # Dovrebbe rispondere 200 OK, non 404
   ```

4. Se ancora 404, i container Spring non sono raggiungibili da Traefik - verificate la network connectivity.
