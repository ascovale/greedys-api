#!/usr/bin/env bash

TIMEOUT=60
HOST=$1
PORT=$2
shift 2
CMD="$@"

for i in $(seq $TIMEOUT); do
    nc -z $HOST $PORT && break
    echo "Waiting for $HOST:$PORT to be ready..."
    sleep 1
done

if [ $i -eq $TIMEOUT ]; then
    echo "Timeout waiting for $HOST:$PORT to be ready"
    exit 1
fi

exec $CMD