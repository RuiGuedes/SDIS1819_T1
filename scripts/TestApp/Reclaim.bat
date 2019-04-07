set CLASS-PATH="..\..\out\production\SDIS1819_T1"
set MAIN="TestApp"
set RMI-ACESS_POINT="rmi-access-point2"
set DISK-SPACE=0

"%JAVA%" --class-path %CLASS-PATH% %MAIN% %RMI-ACESS_POINT% "RECLAIM" %DISK-SPACE%

cmd \k