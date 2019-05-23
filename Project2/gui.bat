@echo off

set TRUSTSTORE="clienttruststore.jks"
set PASSWORD="senorrestive"

set CLASS-PATH="out/production/P2P Backup Service on Internet"
set CLIENT-MAIN="client.GUI"

set PORT="8000"

java -Djavax.net.ssl.trustStore=%TRUSTSTORE% -Djavax.net.ssl.trustStorePassword=%PASSWORD% --class-path %CLASS-PATH% %CLIENT-MAIN% %PORT%