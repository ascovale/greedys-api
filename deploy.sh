#!/bin/bash

docker pull registry.gitlab.com/psychoorange/greedys_api:latest

docker service rm greedys_api_spring-app 

docker stack deploy -c docker-compose.yml greedys_api
