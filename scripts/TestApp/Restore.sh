CLASS_PATH=../../out/production/SDIS1819_T1
MAIN="TestApp"
RMI_ACCESS_POINT="rmi-access-point1"
FILE=../../files/img.jpg

java --class-path $CLASS_PATH $MAIN $RMI_ACCESS_POINT "RESTORE" $FILE
