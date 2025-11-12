# 🔐 RabbitMQ + Docker Swarm Secrets Setup

## Overview

Il setup di RabbitMQ su Docker Swarm richiede:
1. **Secrets creati UNA SOLA VOLTA** sul server (manualmente)
2. **docker-compose.yml** riferisce ai secrets
3. **Pipeline GitLab** fa il deploy (non crea secrets)

---

## ✅ STEP 1: Setup Secrets sul Server (MANUALE)

### Prerequisiti:
- SSH accesso al server manager
- Docker Swarm inizializzato (`docker swarm init`)

### Esecuzione:

```bash
# 1. SSH nel server
ssh deployer@YOUR_SERVER_IP

# 2. Clone/scarica setup-secrets.sh nella home
# (già incluso nel repo)

# 3. Esegui lo script UNA SOLA VOLTA
bash setup-secrets.sh
```

**Output atteso:**
```
✅ Secret 'db_password' created
✅ Secret 'email_password' created
✅ Secret 'jwt_secret' created
✅ Secret 'service_account' created
✅ Secret 'rabbitmq_user' created
✅ Secret 'rabbitmq_password' created
```

### Verifica:
```bash
docker secret ls
```

**Output:**
```
ID                          NAME                    DRIVER              CREATED
abc123...                   db_password             -                   1 minute ago
def456...                   email_password          -                   1 minute ago
ghi789...                   jwt_secret              -                   1 minute ago
jkl012...                   service_account         -                   1 minute ago
mno345...                   rabbitmq_user           -                   1 minute ago
pqr678...                   rabbitmq_password       -                   1 minute ago
```

---

## 🐳 STEP 2: Docker Compose con RabbitMQ

Il `docker-compose.yml` è già aggiornato con:

### Servizio RabbitMQ:
```yaml
rabbitmq:
  image: rabbitmq:3.13-management-alpine
  environment:
    RABBITMQ_DEFAULT_USER_FILE: /run/secrets/rabbitmq_user
    RABBITMQ_DEFAULT_PASS_FILE: /run/secrets/rabbitmq_password
    RABBITMQ_DEFAULT_VHOST: /greedys
  volumes:
    - rabbitmq_data:/var/lib/rabbitmq
    - ./rabbitmq/rabbitmq.conf:/etc/rabbitmq/rabbitmq.conf:ro
  secrets:
    - rabbitmq_user
    - rabbitmq_password
  ports:
    - "5672:5672"    # AMQP
    - "15672:15672"  # Management UI
```

### Spring App (variabili RabbitMQ):
```yaml
spring-app:
  environment:
    SPRING_RABBITMQ_HOST: rabbitmq
    SPRING_RABBITMQ_PORT: 5672
    SPRING_RABBITMQ_USERNAME_FILE: /run/secrets/rabbitmq_user
    SPRING_RABBITMQ_PASSWORD_FILE: /run/secrets/rabbitmq_password
    SPRING_RABBITMQ_VIRTUAL_HOST: /greedys
```

### Volumes e Secrets:
```yaml
volumes:
  rabbitmq_data:  # ← Persistenza RabbitMQ

secrets:
  rabbitmq_user:
    external: true
  rabbitmq_password:
    external: true
```

---

## 🚀 STEP 3: Pipeline GitLab (Deploy)

La pipeline **NON crea i secrets** (già esistono), semplicemente fa il deploy:

```yaml
deploy_prod:
  script:
    - scp docker-compose.yml deployer@SERVER:/greedys_api/
    - scp -r rabbitmq/ deployer@SERVER:/greedys_api/
    - ssh deployer@SERVER 'cd ~/greedys_api && bash deploy.sh'
```

**deploy.sh contiene:**
```bash
docker stack deploy -c docker-compose.yml greedys_api
```

---

## 📊 Workflow Completo

```
┌─ INITIAL SETUP (una volta) ─────────────────────┐
│                                                 │
│  1. ssh deployer@SERVER                         │
│  2. bash setup-secrets.sh                       │
│  3. docker secret ls (verifica)                 │
│                                                 │
└─────────────────────────────────────────────────┘
                        ↓
┌─ EVERY DEPLOYMENT (via GitLab pipeline) ───────┐
│                                                 │
│  1. Build Docker image                          │
│  2. Push to registry                            │
│  3. SCP docker-compose.yml + rabbitmq/          │
│  4. SSH: bash deploy.sh                         │
│     └─> docker stack deploy                    │
│                                                 │
└─────────────────────────────────────────────────┘
                        ↓
┌─ RESULT ───────────────────────────────────────┐
│                                                 │
│  ✅ RabbitMQ running                            │
│  ✅ Spring App connected                        │
│  ✅ Secrets secured in Swarm                    │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## 🔒 Dove Vengono Memorizzati i Secrets

**In Docker Swarm:**
- Memorizzati in `/var/lib/docker/swarm/raft/wal/`
- Cifrati a riposo
- Disponibili SOLO ai container del servizio
- NON sono in chiaro nei file!

**Nel Container:**
- Disponibili come file: `/run/secrets/rabbitmq_user`
- Spring Boot li legge tramite `_FILE` env vars

---

## 🛠️ Manutenzione dei Secrets

### Cambiare RabbitMQ Password:
```bash
# 1. Rimuovi il secret vecchio
docker secret rm rabbitmq_password

# 2. Crea il nuovo
echo "new_password" | docker secret create rabbitmq_password -

# 3. Redeploy lo stack (i container si riavviano)
docker stack deploy -c docker-compose.yml greedys_api
```

### Aggiungere Service Account (Firebase):
```bash
# Se hai un file service-account.json:
cat /path/to/service-account.json | docker secret create service_account -

# Oppure aggiorna il vecchio:
docker secret rm service_account
cat /path/to/service-account.json | docker secret create service_account -
docker stack deploy -c docker-compose.yml greedys_api
```

### Listar i Secrets:
```bash
docker secret ls
```

### Inspezionare un Secret (ATTENZIONE: mostra il valore!):
```bash
# DANGER: Mostra il valore in chiaro!
docker secret inspect rabbitmq_password --pretty
```

---

## 📞 RabbitMQ Access

### Management UI (Web Dashboard):
```
URL: https://rabbitmq.greedys.it
Username: admin (vedi deploy notes)
Password: (stored in secret)
```

### AMQP (Interno Swarm - Spring Boot):
```
Host: rabbitmq (service name)
Port: 5672
User: (rabbitmq_user secret)
Pass: (rabbitmq_password secret)
VHost: /greedys
```

### AMQP (Esterno - se esposto):
```
URL: amqp://greedys:PASSWORD@SERVER:5672/%2Fgreedys
```

---

## ✨ Checklist di Deployment

- [ ] SSH nel server
- [ ] Eseguire `bash setup-secrets.sh`
- [ ] Verificare secrets: `docker secret ls`
- [ ] Verificare docker-compose.yml esiste
- [ ] Verificare rabbitmq/ folder esiste
- [ ] Eseguire `bash deploy.sh`
- [ ] Verificare service: `docker service ls`
- [ ] Verificare logs: `docker service logs greedys_api_rabbitmq`
- [ ] Testare connection: `docker service logs greedys_api_spring-app | grep AMQP`
- [ ] Accedere a Management UI: `https://rabbitmq.greedys.it`

---

## 🐛 Troubleshooting

### RabbitMQ non avvia:
```bash
docker service logs greedys_api_rabbitmq
# Controlla se secrets esistono:
docker secret ls
```

### Spring App non si connette a RabbitMQ:
```bash
docker service logs greedys_api_spring-app | grep -i rabbit
# Verifica variabili d'ambiente:
docker service inspect greedys_api_spring-app | grep RABBITMQ
```

### Secrets non trovati:
```bash
docker secret ls
# Se mancano, esegui:
bash setup-secrets.sh
```

---

## 📚 Files Modifati/Creati

| File | Descrizione |
|------|-------------|
| `docker-compose.yml` | ✅ Aggiunto servizio rabbitmq, secrets, volumes |
| `.gitlab-ci.yml` | ✅ Rimosso creazione secrets (solo deploy) |
| `rabbitmq/rabbitmq.conf` | ✅ Creato file config RabbitMQ |
| `setup-secrets.sh` | ✅ Creato script setup secrets (MANUALE) |

---

## 🎯 Summary

| Responsabilità | Chi | Quando |
|---|---|---|
| **Creare secrets** | Operatore/DevOps (manuale) | **UNA SOLA VOLTA** sul server |
| **Deploy stack** | GitLab pipeline | **Ogni commit** su main |
| **Aggiornare app** | GitLab pipeline | **Automatico** |
| **Mantener secrets** | Operatore/DevOps (manuale) | **Se necessario** (cambio password) |

**Non toccare secrets dalla pipeline!** ✅

