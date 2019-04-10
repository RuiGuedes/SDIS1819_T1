#!/bin/bash

CLASS_PATH="../../out/production/SDIS1819_T1"
MAIN="Peer"
PROTOCOL_VERSION=$1
SERVER_ID=$2
RMI_ACCESS_POINT=$3

java --class-path $CLASS_PATH $MAIN $PROTOCOL_VERSION $SERVER_ID $RMI_ACCESS_POINT "224.0.0.4:4441" "224.0.0.5:4442" "224.0.0.6:4443"