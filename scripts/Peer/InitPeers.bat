SET PROTOCOL-VERSION="1.0"

start /MAX RMI.bat

for /l %%x in (1, 1, 2) do (
   start /MAX Peer.bat %PROTOCOL-VERSION% %%x "rmi-access-point%%x"
)