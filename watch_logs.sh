#!/bin/bash
echo "🔍 Monitoraggio log Docker in tempo reale..."
echo "Premi CTRL+C per fermare"
ssh -i ~/.ssh/id_rsa_work deployer@46.101.209.92 "docker logs greedys_api_spring-app.1.psncf9khial9jfr17d15jqk2l -f"
