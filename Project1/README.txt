################
## INTRODUÇÃO ##
################

-> O presente documento serve para explicitar como compilar e executar as diversas funcionalidades desenvolvidas para o primeiro projecto da cadeira de Sistemas Distribuídos.

-> Para facilitar todo o processo, quer de compilação quer de execução, foram desenvolvidos scripts. De modo a manter a integridade e coerência dos scripts alerta-se para o facto de não alterar a organização de diretórios.

-> Todos os scripts desenvolvidos encontram-se dentro da pasta "scripts".

################
## Compilação ##
################

-> Execução do script "Compile.sh"

########################
## EXECUÇÃO DOS PEERS ##
########################

-> Execução do script "InitPeers.sh". Através deste script podem ser criados inúmeros Peers cujo identificador é sequencial. 

-> Para alterar as definições de execução como a versão do protocolo, endereços e portas multicast é necessário alterar os scripts "InitPeers.sh" e "Peer.sh" respetivamente.

-> A execução do script "InitPeers.sh" automaticamente inicia o rmiregistry.

#########################
## EXECUÇÃO DA TESTAPP ##
#########################

-> Para a execução da TestApp foram desenvolvidos 5 scripts distintos que permitem efetuar as seguintes operações: backup, restore, delete, reclaim e state.

-> Todos estes scripts são executados sem fornecer argumentos. Deste modo para alterar as definições de execução torna-se necessário editar as macros definidas nesses mesmos ficheiros.
