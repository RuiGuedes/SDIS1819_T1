package chord;

public class Chord {
    /*
        -> Supostamente não é necessária existir uma classe Ring que contem todos os nós. A ideia passa por ao iniciar
        um peer criar o seu próprio no com base nos ja existentes. Assim para o peer o peer iniciador apenas é criado e
        o seu sucessor e antecessor é ele proprio. No entanto para os restantes peers na inicialização do nó é
        necessário passar um outro nó já criado. Assim estes nos são apresentados diretamente e permite que novo consiga
        saber o seu antecessor e predecessor (1)

        -> Supostamente o chord utiliza consistent hashing, ou seja: id_nó = truncate(SHA-1(id_peer + peer_port)). O
        resultado do SHA-1 tem de ser truncado para <m> bits. Deste modo podem existir ids na gama [0, 2^m - 1]. O valor
        de é então depois utilizado para criar a finger table de tamanho <m> em que cada elemento é: fingerTable(i) >= id_nó + 2^i(mod 2^m).
        No entanto para depois dar backup de files e saber onde eles se encontram estes tem se ser hashed seguindo a
        mesmo racicionio que os peers de modo a facilmente determinar onde dar backup ou que nó onde esta guardado o chunk (2)

        -> Após desenvolvidas as funçoes relativas ao calculo de successor e do predecessor penso que seja facil desenvolver
        o fault tolerance. Basicamente em vez de dar backup do chunk apenas num nó, dá tambem em <r> successors e predecessors.
        r = 2 log(N) em que N é o numero de nós. No entanto para simplificarmos caso complique podemos dar apenas backup no sucessor
        e no predecessor (3). Como alternativa de modo a ter fault tolerance para o delete podemos usar a introdução de um nivel de direção
        que basicamente em vez de darmos backup dos chunks damos backup de um pointer para onde file se encontra.

        -> Quando novos peer se juntam é necessário ocorrerem atualizações de sucessores, predecessors e finger tables. É preciso ter
        também atençao ao facto de o novo peer ter de ter uma copia de todos os ficheiros guardados no seu sucessor cujo o seu ID seja
        menor ou igual ao id do novo peer. (4)
     */


    /*
        (1) - Como é que sabemos qual o nó a passar ?
        (2) - A função de hash tem de ser igual para os chunks e para o peer. Que tamanho ideal para <m> ?
        (3) - O valor de <r> tem em conta o numero de nós do sistema. Como saber o esse numero ?
        (4) - Isto implica que após o join seja feito a transferencia dos ficheiros necessários o que pode ser chato
     */
}

