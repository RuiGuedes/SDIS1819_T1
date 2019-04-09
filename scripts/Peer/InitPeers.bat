SET PROTOCOL-VERSION="2.0"

start /MAX RMI.bat

for /l %%x in (1, 1, 3) do (
   start /MAX Peer.bat %PROTOCOL-VERSION% %%x "rmi-access-point%%x"
)