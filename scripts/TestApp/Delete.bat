set CLASS-PATH="..\..\out\production\SDIS1819_T1"
set MAIN="TestApp"
set RMI-ACESS_POINT="rmi-access-point1"
set FILE="..\..\files\FileTest.txt"

"%JAVA%" --class-path %CLASS-PATH% %MAIN% %RMI-ACESS_POINT% "DELETE" %FILE%

cmd \k