#!/bin/bash

NETWORK_NAME="testnetwork"

if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  echo "Creating network: $NETWORK_NAME"
  docker network create "$NETWORK_NAME"
else
  echo "Network $NETWORK_NAME already exists"
fi
