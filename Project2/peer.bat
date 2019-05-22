@echo off

set KEYSTORE="peerkeystore.jks"
set PASSWORD="senorrestive"

set CLASS-PATH="out/production/P2P Backup Service on Internet"
set PEER-MAIN="peer.Peer"

set CLIENT-PORT="8000"

start "Peer" java -Djavax.net.ssl.keyStore=%KEYSTORE% -Djavax.net.ssl.keyStorePassword=%PASSWORD% --class-path %CLASS-PATH% %PEER-MAIN% %CLIENT-PORT%