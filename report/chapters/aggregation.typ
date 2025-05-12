#import "/utils.typ": image_block 

= Servidor de Agregação

Este componente tem como principal objetivo encontrar o conjunto de servidores de _chat_ que se encontram em melhores condições para servir um certo tópico, para isso é definida uma política de eleição onde é privilegiado o número de tópicos suportados até ao momento, a quantidade de utilizadores conectados, e por fim o próprio identificador do servidor.

O algoritmo de agregação exige a partilha de informação com os vizinhos, de modo a que todos os nós cheguem à mesma conclusão no final, assim sendo é necessário criar uma _overlay_ que possibilite um _broadcast_ a partir duma _view_ limitada.

== Cyclon

Para gerir esta mesma _overlay_, é aplicada uma versão bastante simplista do _Cyclon_ onde os nós selecionam aleatoriamente um dos seus vizinhos e iniciam de imediato um _shuffle_, de relembrar que não é permitido responder a um _shuffle_ alheio quando ainda não terminei um iniciado por mim, sendo portanto devolvido um _Cyclon Error_.

Devido à utilização de _sockets ZeroMQ_, a deteção de mortes por perda de ligação torna-se num grande desafio, sendo preciso implementar mecanismos de _heartbeats_, algo que certamente contribuiria para aumentar a complexidade da implementação.

Neste sentido, optámos por não implementar o esquema de _Enhanced Shuffling_, pois os vizinhos mais antigos seriam selecionados para realizar o _shuffling_ e provavelmente não iriam responder devido à sua morte, consequentemente a _view_ teria sempre os mesmos elementos. Uma vez não detetada a morte de vizinhos, a nossa versão de _shuffling_ propaga vizinhos mortos, algo que reconhecemos como sendo uma debilidade do sistema.

=== Entrada na Overlay

No momento em que um servidor de agregação é iniciado, o mesmo contacta de imediato um _entry point_ previamente conhecido e inicia uma troca de vizinhos, ficando por isso a conhecer os vizinhos do _entry point_.

Apesar de simples, esta abordagem acarreta a desvantagem do _entry point_ ser extremamente popular, como tal a probabilidade do mesmo estar presente na _view_ dos restantes nós é bastante elevada, o que contribui para uma _overlay_ pouco saudável. Para resolver este problema seria útil aplicar o algoritmo de _SCAMP_ referido no _paper_ do _Cyclon_, mas devido à falta de tempo não foi possível investir nesse ponto. 

== Extrema Propagation

A escolha dos servidores de _chat_ que devem suportar um determinado tópico é conseguida somente através de disseminações, visto que o estado do _SCx_ é obtido pelo _SAx_, além disso é expectável que no final das agregações todos os envolvidos tenham chegado à mesma conclusão.

De modo a atingir estes objetivos, é aplicado o algoritmo de _Extrema Propagation_ onde cada nó obtém o estado local e propaga-o para todos os seus vizinhos, eventualmente a única mensagem transmitida será a lista _top-C_ dos servidores de _chat_, e portanto todos os _SA_ irão chegar à mesma conclusão.

O desafio deste algoritmo consiste em saber quando terminar, para isso definimos um _T_ que indica a convergência, ou seja, quando a lista _top-C_ local não sofrer _T_ alterações consecutivas, isso implica a convergência, visto ser pouco provável futuramente virmos a obter um _top-C_ melhor.

Numa outra perspetiva, o nó que inicia a agregação tem de possuir um _T_ menor que os restantes, pois como os demais podem convergir primeiro, deixam de transmitir mensagens e portanto corremos o risco de não convergir. Deste modo, para todos os nós à exceção daquele que iniciou a agregação, definimos $T = T + T/2$.

== Arquitetura

Tendo em conta que a troca de vizinhos deve ser realizada periodicamente, o servidor de agregação conta com um componente especializado na emissão de pedidos de _shuffle_, algo que é reencaminhado para um _buffer_ e mais tarde será entregue sem quaisquer garantias de ordenação. Por fim, as lógicas de agregação e _merge_ das _views_ é realizada num único componente, ao qual todas as mensagens recebidas acabam por ser entregues. 

#image_block(
  imagem: image("/images/aggr_arch.png"),
  caption: [Arquitetura do Servidor de Agregação]
)

De modo a comunicar com os vizinhos, o grupo pensou em aplicar o paradigma `Router-Router`, dado ser possível conectar um _socket_ a vários endereços e enviar mensagens para um alvo específico, no entanto percebemos que a _lib jeromq_ apresenta alguns _bugs_, e mudámos para o paradigma `Push-Pull`.


Assim sendo, todas as mensagens enviadas pelos vizinhos são recebidas no `Pull`, enquanto o envio das mesmas exige a utilização de vários `Push`, pois a lógica dos algoritmos de disseminação e _shuffle_ exige o envio para um alvo concreto.

Por fim, de modo a responder aos pedidos de agregação requisitados por clientes, é disponibilizado um _socket_ do tipo `Rep`, daí que um servidor não possa iniciar várias agregações em paralelo, no entanto é possível responder a múltiplas agregações para tópicos diferentes em paralelo.

Além disso é utilizado outro _socket_ do mesmo tipo que está conectado ao servidor de _chat_ correspondente, algo que permite a obtenção de estado interno, bem como a comunicação de que o servidor em questão passou a servir um novo tópico.