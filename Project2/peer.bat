@echo off

set KEYSTORE="peerkeystore.jks"
set TRUSTSTORE="clienttruststore.jks"
set PASSWORD="senorrestive"

set CLASS-PATH="out/production/P2P Backup Service on Internet"
set PEER-MAIN="peer.Peer"

start "Peer" java -Djavax.net.ssl.keyStore=%KEYSTORE% -Djavax.net.ssl.keyStorePassword=%PASSWORD% -Djavax.net.ssl.trustStore=%TRUSTSTORE% -Djavax.net.ssl.trustStorePassword=%PASSWORD% --class-path %CLASS-PATH% %PEER-MAIN% %*