#!/bin/bash

PROTOCOL_VERSION="2.0"

gnome-terminal --execute ./RMI.sh

for i in {1..2}
do
   gnome-terminal --execute ./Peer.sh $PROTOCOL_VERSION $i "rmi-access-point$i"
done

