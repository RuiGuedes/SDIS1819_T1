set CLASS-PATH="..\..\out\production\SDIS1819_T1"
set MAIN="Peer"
set PROTOCOL-VERSION=%1
set SERVER-ID=%2
set RMI-ACESS_POINT=%3

"%JAVA%" --class-path %CLASS-PATH% %MAIN% %PROTOCOL-VERSION% %SERVER-ID% %RMI-ACESS_POINT% "224.0.0.4:4441" "224.0.0.5:4442" "224.0.0.6:4443"
cmd /k