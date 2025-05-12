#import "/utils.typ": todo 

//#todo[colocar os termos estrangeiros em itálico]

//#todo[chat room deve ser substituido por topico (manter consistência com o resto do relatório)]

//#todo[No fim, colocar a pontuação, nomeadamente acentos (eu trato disso)]

= Servidor de Pesquisa

Este componente gere uma _key-value store_ que associa um tópico aos servidores de _chat_ que o suportam, para isso é utilizada uma _DHT_ do tipo _Consistent Hashing_, à qual os clientes recorrem para entrar num determinado tópico (operação de leitura), enquanto os servidores de agregação registam os endereços das máquinas selecionadas pelo algoritmo de agregação (operação de escrita).

// == Estratégia


// Na implementação dos servidores de pesquisa, seguimos uma abordagem simplificada da DHT Chord, em que não são suportadas remoções de nós, e não são usados _fingers_ para otimizar o numero de conexões necessárias.

// Assim, a nossa DHT suporta inserções de novos nós, e todos os nós têm ligação para todos os outros nós (NxN), no entanto com um custo O(1) em praticamente todas as operações. Usamos JSON e TCP para todas as comunicações envolvendo os servidores de pesquisa, sem exceção.

// #todo[acho que podes apagar esta subchapter todo, pois na fase introdutória diz que a DHT é do tipo consistent hashing, algo que é auto-explicativo para quem conhece o conceito (sugestão: apagar este subchapter) ]

== Funcionamento

=== Dados

Em cada nó, são guardados os _ID_ e _hash_ próprios, bem como do sucessor, além disso é preservada uma lista ordenada das _hashes_ de todos os participantes (incluindo nós próprios) e um _map_ que faz essas _hashes_ corresponderem a um certo _ID_. Com estas informações, cada servidor tem uma visão global do estado da _DHT_.

// Cada nodo guarda a sua própria hash/ID, a hash/ID do seu sucessor, e um map que faz corresponder a hash de todos os nós ao seu ID (incluindo-se ele próprio), ao qual se associa uma lista de hashes ordenada da menor hash para a maior.

// Para a descrição dos algoritmos, considere-se que cada nodo contem estes dados:

// - `hashes`: lista ordenada de todas as hashes de todos os nós na DHT (da menor hash para a maior)
// - `hash_id`: map que faz corresponder o ID do nodo, dada a sua hash
// - `succ`: id do sucessor

// #todo[esta a ser dita a mesma coisa de formas diferentes, uma em texto corrido, e outra em lista, sugestão (menter o texto corrido, talvez melhorar, e apagar esta lista)]

=== GET IPs

Percorre-se a lista de _hashes_, comparando cada uma com a _hash_ do tópico requisitado. Assim que seja encontrada uma _hash_ maior que a do tópico, considera-se o nó correspondente como sendo responsável pelo mesmo. No cenário em que a _hash_ é muito grande (`FFFF`) e o único servidor disponível está do 'outro lado' da _DHT_ (`0000`), nenhuma _hash_ será maior que o tópico, considerando-se por isso a primeira _hash_ disponível na lista.

Uma vez determinado o responsável pelo tópico, o servidor devolve de imediato os resultados ao cliente, quer este seja verdadeiramente um cliente, ou outro servidor de pesquisa que teve de realizar um _hop_ para encontrar o valor associado à _hash_ do tópico.

=== PUT IPs

De modo análogo ao procedimento anteriormente descrito, a operação de escrita na _DHT_ segue uma lógica semelhante, ou seja, numa primeira fase é identificado o servidor de pesquisa associado à _hash_ do tópico, e de seguida esse mesmo participante escreve o valor em memória local.

=== Entrada na DHT

Quando pensamos da inserção de participantes na _DHT_, devemos procurar respeitar as propriedades de _monotonicity_, deste modo a partilha de informação entre servidores será reduzida ao máximo, visto que a realocação de chaves não atinge todos os nós. Para isso formulámos um algoritmo de entrada descrito nos seguintes passos:



//As entradas decorrem em 3 etapas:

// - Determinar o sucessor
// - Conectar ao sucessor e sincronizar dados
// - Sincronizar dados
// - Conectar aos restantes nós
#pagebreak()

1. *Determinar o sucessor*

Para facilitar entradas na _DHT_, existe um pedido com o qual um nó pode perguntar a outro quem ele acredita ser o sucessor de quem originou o pedido. A lógica usada para o determinar é exatamente igual à previamente referida para determinar um responsável dada uma _hash_, usando-se $text("hash do nó") + 1$.

2. *Sincronizar dados - conectar ao sucessor*

Quando um participante estabelece conexão com outro, identificando-se pelo seu _ID_, imediatamente ambos atualizam os seus dados com base no conhecimento de que existe um novo servidor.

Existe um caso particular da conexão de nós, em que um nó se conecta ao sucessor. Este sucessor irá responder com a lista de todos os seus vizinhos, bem como os dados que agora passam a pertencer ao novo nó. Este último passo é realizado percorrendo todos os dados e determinando a qual nó pertence, usando o algoritmo anteriormente descrito. Após esta operação, ambos os nós têm apenas os dados de que são responsáveis segundo as suas _hashes_.

3. *Conectar aos restantes nós*

Após os dados terem sido sincronizados com o sucessor, o novo participante conecta-se aos restantes, transmitindo-lhes, assim, o facto de que um novo nó foi inserido.

No caso de pedidos em que a _hash_ de um tópico coincide com este novo nó, mas o nó que recebeu o pedido ainda não tem conhecimento sobre a inserção, o pedido será necessariamente reencaminhado para o sucessor, que por sua vez tem, inevitavelmente, conhecimento sobre a inserção, e reencaminhará os dados para o nó correto.
// Assim, no pior dos casos a única consequência será haverem 2 hops em vez de 1 ou 0.

// #todo[não cabe mais nada, se achares que existe algo mais importante que aquilo que já está escrito, podes substituir, acho que seria vantajoso colocar uma imagem]







// IMAGENS

// 1 - inicialmente temos o nó 0 e os chat rooms r0 e r3. Toda a gama de hashes possivel cai na responsabilidade do nó 0.

// 2 - adicionamos o nó 1, que divide esta gama de valores, representada por cores. observamos que `r1` se mantem na responsabilidade de `N0`, como esperado, e que `r3` foi migrado para o `N1`

// 3 - o room r5, que fica na zona do nó 0. adicionam-se nós 2, 3 e 4. observa-se, mais uma vez, a correta migracao do r0 para o nodo 4, e como o r5 se mantem no nodo 0

// protocolo json para tudo, por tcp

// igual a um chord, mas sem as otimizacoes de fingers, usando uma mesh de NxN conexoes, e sem permitir remocoes

// inicialmente existe apenas um nodo 0

// um novo nodo (1) conecta-se ao mesmo, perguntando quem o nodo 0 acha ser o sucessor deste novo nodo (vai ser o nodo 0)

// liga se ao sucessor (0) e pede para lhe enviar os dados que agora sao dele, bem como a lista que conhece de todos os nós

// integra os novos dados, e faz uma conexao para cada um dos outros nós (neste caso nenhum)

// se houverem outros nós, estes irao aprender da insercao atraves desta conexao

// se um nodo quiser realizar operacoes sobre dados que agora sao responsabilidade do nodo inserido, mas que ainda nao sabem sobre a insercao, irao contactar o sucessor do nodo inserido (sem saberem que se trata de um sucessor), e como esse nodo obrigatoriamente conhece a insercao, ira redirecionar o pedido
