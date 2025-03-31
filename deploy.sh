#!/bin/bash


docker build -t greedys_api_spring-app .

docker service rm greedys_api_spring-app 

docker stack deploy -c docker-compose.yml greedys_api
