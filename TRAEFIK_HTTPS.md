# Traefik HTTPS Configuration

## Overview

This project uses **Traefik v3.0** as a reverse proxy with **Let's Encrypt** for automatic HTTPS certificate management in Docker Swarm mode.

## Architecture

```
Internet (HTTPS port 443)
         ↓
    Traefik v3.0 (Reverse Proxy)
    - SSL/TLS Termination
    - Let's Encrypt ACME
         ↓
Spring Boot API (HTTP port 8080)
MySQL Database (internal)
```

## Docker Files

### 1. **docker-compose.yml** (Main Configuration)
- **Location**: Repository root (`greedys_api/`)
- **Purpose**: Production Docker Swarm deployment configuration
- **Services**:
  - `traefik`: Reverse proxy and SSL termination
  - `spring-app`: Spring Boot API (HTTP internal)
  - `db`: MySQL 8.0 database
- **Network**: `app-network` (overlay network for Swarm)
- **Usage**: Deployed via pipeline with `docker stack deploy`

### 2. **Traefik Configuration Files** (Server Only)

Located on the production server at: `/opt/greedys_api/traefik/`

#### a) **traefik.yml** (Main Traefik Config)
- **Purpose**: Main Traefik configuration
- **Contents**:
  - Entry points: HTTP (80), HTTPS (443), Dashboard (8080)
  - API configuration
  - Let's Encrypt ACME resolver setup
  - Log levels
- **Generated**: Automatically during HTTPS setup
- **On Server**: `/opt/greedys_api/traefik/traefik.yml`

#### b) **dynamic.yml** (Dynamic Configuration)
- **Purpose**: Runtime configuration for routes, middleware, and TLS options
- **Contents**:
  - HTTP to HTTPS redirect middleware
  - Security headers (HSTS, CSP, X-Frame-Options, etc.)
  - CORS middleware
  - Rate limiting
  - TLS minimum version (1.2)
  - Cipher suites
- **Generated**: Automatically during HTTPS setup
- **On Server**: `/opt/greedys_api/traefik/dynamic.yml`

#### c) **acme.json** (Certificate Storage)
- **Purpose**: Stores Let's Encrypt certificates and keys
- **File Type**: JSON
- **Auto-populated**: Yes, during first deployment when ACME challenges are solved
- **Permissions**: `600` (read/write only by Traefik)
- **Renewal**: Automatic (30 days before expiration)
- **On Server**: `/opt/greedys_api/traefik/acme.json`
- **Mounted in Docker**: Read/write by Traefik service

## Let's Encrypt Configuration

### ACME Settings (in docker-compose.yml)
```yaml
environment:
  - TRAEFIK_CERTIFICATESRESOLVERS_LETSENCRYPT_ACME_EMAIL=beeinggreedy@gmail.com
  - TRAEFIK_CERTIFICATESRESOLVERS_LETSENCRYPT_ACME_STORAGE=/acme.json
  - TRAEFIK_CERTIFICATESRESOLVERS_LETSENCRYPT_ACME_HTTPCHALLENGE_ENTRYPOINT=web
```

### Certificate Validation
- **Method**: HTTP Challenge (port 80)
- **Domain**: `api.greedys.it`
- **Auto-renewal**: Triggered at 30 days before expiration
- **Cost**: FREE (Let's Encrypt)

## Service Labels (docker-compose.yml)

### Spring API Service Labels
```yaml
labels:
  - "traefik.enable=true"
  - "traefik.http.routers.api.rule=Host(`api.greedys.it`)"
  - "traefik.http.routers.api.entrypoints=web,websecure"
  - "traefik.http.routers.api.service=api"
  - "traefik.http.routers.api.tls.certresolver=letsencrypt"
  - "traefik.http.routers.api.middlewares=cors@file,security@file,rate-limit@file"
```

**Explanation**:
- Route HTTP and HTTPS traffic to `api.greedys.it`
- Enable TLS with Let's Encrypt resolver
- Apply security middleware from `dynamic.yml`

## Deployment Process

### Initial Deployment
1. **Pipeline triggers** → Builds Docker image
2. **Deploy stage** transfers:
   - `docker-compose.yml` (updated with Traefik)
   - `deploy.sh` (deployment script)
3. **deploy.sh** executes:
   ```bash
   docker pull registry.gitlab.com/greedysgroup/greedys_api:latest
   docker stack rm greedys_api  # Remove old stack
   sleep 10                      # Wait for cleanup
   docker stack deploy -c docker-compose.yml greedys_api
   ```
4. **Docker Swarm** creates:
   - Traefik service (manager node)
   - Spring API service
   - MySQL database (manager node)
   - Overlay network `app-network`

### Certificate Generation
1. Traefik starts and reads `traefik.yml`
2. Receives first HTTPS request to `api.greedys.it`
3. Initiates HTTP challenge on port 80
4. Let's Encrypt validates and issues certificate
5. Certificate stored in `/acme.json`
6. **Timeline**: 5-10 minutes from first deployment

### Monitoring Certificate Status
```bash
docker service logs -f greedys_api_traefik | grep -i certificate
```

Expected output: `Certificate obtained for domain [api.greedys.it]`

## File Structure on Server

```
/opt/greedys_api/
├── docker-compose.yml          # Main config (from pipeline)
├── deploy.sh                    # Deployment script (from pipeline)
├── traefik/
│   ├── traefik.yml            # Traefik main config
│   ├── dynamic.yml            # Routes, middleware, TLS
│   ├── acme.json              # Certificates (auto-generated)
│   └── localhost.*            # Legacy test certs (can be removed)
└── secrets/                    # Docker Swarm secrets directory
```

## Key Features

| Feature | Details |
|---------|---------|
| **SSL/TLS** | Automatic with Let's Encrypt |
| **Certificate Renewal** | Automatic (30 days before expiry) |
| **HTTP Redirect** | All HTTP traffic → HTTPS |
| **Security Headers** | HSTS, CSP, X-Frame-Options enabled |
| **Rate Limiting** | Applied via middleware |
| **Domain** | `api.greedys.it` |
| **Email** | beeinggreedy@gmail.com |
| **Port 80** | HTTP (ACME challenge + redirect) |
| **Port 443** | HTTPS (production traffic) |
| **Port 8080** | Traefik Dashboard (internal) |

## Troubleshooting

### Certificate Not Generated
```bash
# Check Traefik logs
docker service logs greedys_api_traefik

# Verify acme.json exists and is readable
ls -la /opt/greedys_api/traefik/acme.json

# Check permissions (should be 600)
stat /opt/greedys_api/traefik/acme.json
```

### HTTPS Not Working
```bash
# Test HTTPS connection
curl -v https://api.greedys.it/swagger-ui.html

# Check service status
docker service ls | grep greedys_api

# Check if Traefik can reach Spring API
docker exec $(docker ps -q -f "ancestor=traefik:v3.0") \
  curl -v http://spring-app:8080/actuator/health
```

### Port 80/443 Already in Use
```bash
# Check what's using the ports
netstat -tlnp | grep ':80\|:443'

# Kill conflicting services
docker service rm <service_name>
```

## Updates and Maintenance

- **No manual intervention needed** for certificate renewal
- **Traefik handles everything automatically**
- **ACME challenges run on port 80** (must be accessible)
- **Certificates valid for 90 days** (renewed at 30-day mark)

## Related Documentation

- Docker Compose Configuration: See `docker-compose.yml`
- Deployment Script: See `deploy.sh`
- Spring Boot Configuration: See `greedys_api/src/main/resources/application-docker.properties`
