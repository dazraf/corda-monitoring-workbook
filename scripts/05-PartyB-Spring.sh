#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="${SCRIPT_DIR}/.."
CLIENT_APP_DIR="${ROOT_DIR}/clients/build/libs"
pushd "${CLIENT_APP_DIR}"

java -jar clients-0.1.jar --server.port=10051 --config.rpc.host=localhost --config.rpc.port=10009 --config.rpc.username=user1 --config.rpc.password=test
