#!/bin/sh

#export DB_NAME=taccaseapi
#export DB_URL=jdbc:postgresql://localhost:6432/taccaseapi
#export DB_USERNAME=tacapiuser
#export DB_PASSWORD=tacapipass
#export AUTH_SERVER_ISSUER-URI=http://localhost:9000

docker compose up -d --remove-orphans --profile development
