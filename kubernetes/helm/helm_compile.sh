#!/bin/bash

RED='\033[0;31m' # Error color
YELLOW='\033[0;33m' # Warning color
NC='\033[0m' # No Color

set -u
DIR="."
GetPathToCurrentlyExecutingScript () {
	# Absolute path of this script, e.g. /opt/corda/node/foo.sh
	set +e
	ABS_PATH=$(readlink -f "$0" 2>&1)
	if [ "$?" -ne "0" ]; then
		echo "Using macOS alternative to readlink -f command..."
		# Unfortunate MacOs issue with readlink functionality, see https://github.com/corda/corda-kubernetes-deployment/issues/4
		TARGET_FILE=$0

		cd $(dirname $TARGET_FILE)
		TARGET_FILE=$(basename $TARGET_FILE)
		ITERATIONS=0

		# Iterate down a (possible) chain of symlinks
		while [ -L "$TARGET_FILE" ]
		do
			TARGET_FILE=$(readlink $TARGET_FILE)
			cd $(dirname $TARGET_FILE)
			TARGET_FILE=$(basename $TARGET_FILE)
			ITERATIONS=$((ITERATIONS + 1))
			if [ "$ITERATIONS" -gt 1000 ]; then
				echo "symlink loop. Critical exit."
				exit 1
			fi
		done

		# Compute the canonicalized name by finding the physical path 
		# for the directory we're in and appending the target file.
		PHYS_DIR=$(pwd -P)
		ABS_PATH=$PHYS_DIR/$TARGET_FILE
	fi

	# Absolute path of the directory this script is in, thus /opt/corda/node/
	DIR=$(dirname "$ABS_PATH")
}
GetPathToCurrentlyExecutingScript

HelmCompilePrerequisites () {
	helm version | grep "v2." > /dev/null 2>&1
	if [ "$?" -ne "0" ] ; then
		echo -e "${RED}ERROR${NC}"
		echo "Helm version 2 has to be used for compiling these scripts. Please install it from https://github.com/helm/helm/releases"
		exit 1
	fi

	kubectl cluster-info > /dev/null 2>&1
	if [ "$?" -ne "0" ] ; then
		echo -e "${RED}ERROR${NC}"
		echo "kubectl must be connected to the Kubernetes cluster in order to continue. Please see https://kubernetes.io/docs/tasks/access-application-cluster/configure-access-multiple-clusters/"
		exit 1
	fi

	set -eu

	TEMPLATE_NAMESPACE=""
	TEMPLATE_NAMESPACE=$(grep -A 3 'config:' $DIR/values.yaml | grep 'namespace: "' | cut -d '"' -f 2)

	if [ "$TEMPLATE_NAMESPACE" = "" ]; then
		echo -e "${RED}ERROR${NC}"
		echo "Kubernetes requires a namespace to deploy resources to, no namespace is defined in values.yaml, please define one."
		exit 1
	fi
	
	if [ ! -f $DIR/files/network/networkRootTrustStore.jks ]; then
		echo -e "${RED}ERROR${NC}"
		echo "$DIR/files/networkRootTrustStore.jks missing, this should have been copied to this folder before running one-time-setup.sh script."
		exit 1
	fi
	
	if [ ! -f $DIR/files/network/network-parameters.file ]; then
		echo -e "${RED}ERROR${NC}"
		echo "$DIR/files/network-parameters.file missing, this should have been created by one-time-setup.sh script."
		exit 1
	fi
	
	if [ ! -f $DIR/files/certificates/node/nodekeystore.jks -o ! -f $DIR/files/certificates/node/sslkeystore.jks -o ! -f $DIR/files/certificates/node/truststore.jks ]; then
		echo -e "${RED}ERROR${NC}"
		echo "$DIR/files/certificates/node/ missing certificates, expecting to see nodekeystore.jks, sslkeystore.jks and truststore.jks, these files should have been created by one-time-setup.sh script."
		echo "Files in folder $DIR/files/certificates/node:"
		ls -al $DIR/files/certificates/node
		exit 1
	fi
	
	if [ ! -f $DIR/files/certificates/firewall_tunnel/bridge.jks -o ! -f $DIR/files/certificates/firewall_tunnel/float.jks -o ! -f $DIR/files/certificates/firewall_tunnel/trust.jks ]; then
		echo -e "${RED}ERROR${NC}"
		echo "$DIR/files/certificates/firewall_tunnel/ missing certificates, expecting to see bridge.jks, float.jks and trust.jks, these files should have been created by one-time-setup.sh script."
		echo "Files in folder $DIR/files/certificates/firewall_tunnel:"
		ls -al $DIR/files/certificates/firewall_tunnel
		exit 1
	fi
}
HelmCompilePrerequisites

HelmCompile () {
	echo "====== Deploying to Kubernetes cluster next ... ====== "
	echo "Compiling Helm templates..."
	helm template $DIR --name $TEMPLATE_NAMESPACE --namespace $TEMPLATE_NAMESPACE --output-dir $DIR/output

	# copy-files script
	SCRIPT="$DIR/output/corda/templates/copy-files.sh"
	mv $SCRIPT.yml $SCRIPT
	# Helm always adds a few extra lines, which we want to remove from shell scripts
	tail -n +3 "$SCRIPT" > "$SCRIPT.tmp" && mv "$SCRIPT.tmp" "$SCRIPT"
	chmod +x $SCRIPT

	echo "Creating Docker Container Registry Pull Secret..."
	# docker secret script
	SCRIPT="$DIR/output/corda/templates/create-docker-secret.sh"
	mv $SCRIPT.yml $SCRIPT
	# Helm always adds a few extra lines, which we want to remove from shell scripts
	tail -n +3 "$SCRIPT" > "$SCRIPT.tmp" && mv "$SCRIPT.tmp" "$SCRIPT"
	chmod +x $SCRIPT
	$SCRIPT

	echo "Applying templates to Kubernetes cluster:"
	kubectl apply -f $DIR/output/corda/templates/ --namespace=$TEMPLATE_NAMESPACE

	# Copy CorDapps, Database drivers etc.
	#$DIR/output/corda/templates/copy-files.sh
	echo "====== Deploying to Kubernetes cluster completed. ====== "
}
HelmCompile
