# 🔧 Fix Traefik Configuration Transfer

## 🔴 Problema Identificato

Traefik non era in grado di avviarsi perché i file di configurazione non erano presenti sul server:
- ❌ `/home/deployer/greedys_api/traefik/traefik.yml` - MANCANTE
- ❌ `/home/deployer/greedys_api/traefik/dynamic.yml` - MANCANTE
- ❌ `/home/deployer/greedys_api/traefik/acme.json` - MANCANTE

**Risultato**: Service status `greedys_api_traefik 0/1` (not running)

```bash
$ docker service ls
ID                  NAME                    MODE                REPLICAS
2k4nq1w2v3x4        greedys_api_traefik     replicated          0/1  ❌ NOT RUNNING
```

**Errore HTTPS**: `502 Bad Gateway` perché Traefik non poteva instradare le richieste

---

## ✅ Soluzione Implementata

### 1. **Aggiornamento `.gitlab-ci.yml`** - Deploy Stage

Aggiunto comando SCP per trasferire la directory `traefik/` al server:

```yaml
deploy:
  script:
    - ssh -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i deploy/id_key deployer@"$SERVER_IP" 'mkdir -p ~/greedys_api/'
    - scp -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i deploy/id_key docker-compose.yml deploy.sh deployer@"$SERVER_IP":~/greedys_api/
    - scp -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -r -i deploy/id_key traefik/ deployer@"$SERVER_IP":~/greedys_api/
    - ssh -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i deploy/id_key deployer@"$SERVER_IP" 'cd ~/greedys_api/ && chmod +x ./deploy.sh && bash ./deploy.sh'
```

**Cambio**: Aggiunta riga SCP ricorsiva per copiare `traefik/`:
```bash
scp -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -r -i deploy/id_key traefik/ deployer@"$SERVER_IP":~/greedys_api/
```

### 2. **Aggiornamento `deploy.sh`** - Creazione Directory & acme.json

Aggiunto setup per la directory traefik e creazione di `acme.json`:

```bash
echo "Checking and creating traefik directory..."
mkdir -p traefik

echo "Creating acme.json if it doesn't exist..."
if [ ! -f traefik/acme.json ]; then
  touch traefik/acme.json
  chmod 600 traefik/acme.json
fi
```

Questo assicura che:
- ✅ La directory `traefik/` esista
- ✅ Il file `acme.json` sia creato con permessi corretti (600)
- ✅ Traefik possa scrivere i certificati Let's Encrypt nel file

---

## 📁 File di Configurazione Traefik

### `traefik/traefik.yml` - Main Configuration
```yaml
global:
  checkNewVersion: false
  sendAnonymousUsage: false

api:
  dashboard: true

entryPoints:
  web:
    address: ":80"
    http:
      redirections:
        entryPoint:
          to: websecure
          scheme: https
  websecure:
    address: ":443"

certificatesResolvers:
  letsencrypt:
    acme:
      email: beeinggreedy@gmail.com
      storage: /ssl-certs/acme.json
      httpChallenge:
        entryPoint: web
```

### `traefik/dynamic.yml` - Dynamic Routing & Middleware
Contiene:
- ✅ CORS middleware per `greedys.it`
- ✅ Security headers (HSTS, etc)
- ✅ Rate limiting
- ✅ TLS options (TLS 1.2/1.3)

---

## 🚀 Prossimi Passi

1. **Commit le modifiche**:
   ```bash
   git add .gitlab-ci.yml deploy.sh
   git commit -m "Fix: Transfer Traefik config files to server in deploy stage"
   git push origin main
   ```

2. **Trigger la pipeline** manualmente o aspetta il prossimo push

3. **Verifica il deploy**:
   ```bash
   ssh deployer@46.101.209.92
   ls -la ~/greedys_api/traefik/
   # Output atteso:
   # -rw-r--r-- traefik.yml
   # -rw-r--r-- dynamic.yml
   # -rw------- acme.json
   ```

4. **Verifica che Traefik si avvii**:
   ```bash
   docker service ls | grep traefik
   # Atteso: greedys_api_traefik replicated 1/1
   ```

5. **Testa HTTPS** (attendi 5-10 minuti per il certificato Let's Encrypt):
   ```bash
   curl -v https://api.greedys.it/v3/api-docs/restaurant-api
   # Atteso: HTTP 200 con certificato valido
   ```

---

## 📊 Timeline Attesa

| Fase | Tempo | Azione |
|------|-------|--------|
| Deploy Stage | ~2 minuti | Trasferimento file, avvio Traefik |
| Traefik startup | ~30 sec | Container init |
| Let's Encrypt ACME | 5-10 min | HTTP challenge, certificato generato |
| **Total HTTPS Ready** | **5-10 minuti** | |

---

## 🔍 Troubleshooting

Se HTTPS ancora non funziona dopo 15 minuti:

```bash
# 1. Verifica che i file siano sul server
ssh deployer@46.101.209.92 "ls -la ~/greedys_api/traefik/"

# 2. Verifica lo stato di Traefik
ssh -i ~/.ssh/id_rsa root@46.101.209.92 "docker service logs greedys_api_traefik | tail -50"

# 3. Verifica che acme.json sia stato creato
ssh deployer@46.101.209.92 "ls -la ~/greedys_api/traefik/acme.json"

# 4. Verifica il certificato generato
ssh deployer@46.101.209.92 "cat ~/greedys_api/traefik/acme.json | grep -i certificate"
```

---

## ✨ Risultato Finale Atteso

```
✅ HTTP  → 200 OK (api.greedys.it:8080)
✅ HTTPS → 200 OK (api.greedys.it) + Certificato Let's Encrypt valido
✅ Traefik → 1/1 replicas running
✅ Dashboard → http://traefik.greedys.it/dashboard (se configurato)
```
