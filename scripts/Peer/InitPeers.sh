#!/bin/bash

PROTOCOL_VERSION="1.0"

gnome-terminal -- ./RMI.sh

for i in {1..5}
do
   gnome-terminal -- ./Peer.sh $PROTOCOL_VERSION $i "rmi-access-point$i"
done

