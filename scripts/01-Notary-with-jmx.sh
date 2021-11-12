#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="${SCRIPT_DIR}/.."
NODES_DIR="${ROOT_DIR}/build/nodes"
echo "Looking for nodes directory $NODES_DIR"

if [ ! -d "${NODES_DIR}" ]
then
  printf "build/nodes directory does not exist.\ndid you forget to run deployNodes?\n"
  exit
fi

pushd $NODES_DIR/Notary
java -jar -Dcapsule.jvm.args="-javaagent:drivers/jolokia-jvm-1.6.0-agent.jar=port=7000" corda.jar
