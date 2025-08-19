#!/bin/bash

CONTAINER_NAME=$1
shift
RUN_ARGS="$@"

if [ -z "$CONTAINER_NAME" ]; then
  echo "Usage: $0 <container-name> [docker run args]"
  exit 1
fi

# Check if container exists
if docker ps -a --format '{{.Names}}' | grep -wq "$CONTAINER_NAME"; then
  # Stop if running
  if docker ps --format '{{.Names}}' | grep -wq "$CONTAINER_NAME"; then
    echo "Stopping container '$CONTAINER_NAME'..."
    docker stop "$CONTAINER_NAME" >/dev/null
  fi

  echo "Starting existing container '$CONTAINER_NAME'..."
  docker start "$CONTAINER_NAME" >/dev/null
else
  echo "Creating and starting container '$CONTAINER_NAME'..."
  docker run -d --name "$CONTAINER_NAME" $RUN_ARGS
fi
