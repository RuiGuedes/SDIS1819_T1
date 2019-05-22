@echo off

set KEYSTORE="peerkeystore.jks"
set PASSWORD="senorrestive"

set CLASS-PATH="out/production/P2P Backup Service on Internet"
set PEER-MAIN="peer.Peer"

set PORT="9002"
set CONTACT_PEER_ADDRESS="192.168.1.184"
set CONTACT_PEER_PORT="9001"

start "Peer" java -Djavax.net.ssl.keyStore=%KEYSTORE% -Djavax.net.ssl.keyStorePassword=%PASSWORD% --class-path %CLASS-PATH% %PEER-MAIN% %PORT% %CONTACT_PEER_ADDRESS%:%CONTACT_PEER_PORT%