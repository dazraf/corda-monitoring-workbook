#!/usr/bin/env bash

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "$SCRIPT_DIR/images"
CORDA_IMAGE_PATH="corda/local"
CORDA_DOCKER_IMAGE_VERSION="4.8.2"
DOCKER_CMD=docker
NO_CACHE=
$DOCKER_CMD build -t $CORDA_IMAGE_PATH:$CORDA_DOCKER_IMAGE_VERSION . -f Dockerfile $NO_CACHE
