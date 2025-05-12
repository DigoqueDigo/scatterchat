#import "/utils.typ": image_block 

= Servidor de Chat

Este componente visa essencialmente a partilha de mensagens ao longo dos diversos servidores, sendo ainda realizada uma retransmissão para todos os clientes que subscrevem um certo tópico, além disso são disponibilizados outros serviços, tais como a obtenção do _log_ relativo ao tópico, e a listagem dos utilizadores _online_.  

== Entrega Causal

Para que o serviço funcione devidamente, é expectável que seja garantida a ordem causal, deste modo, a mensagem _Y_, que é uma resposta a _X_, nunca será entregue antes de _X_, caso contrário seria uma resposta a algo que ainda não existe. Por outro lado, convém realçar que a ordem total não é preservada, consequentemente os clientes podem ver a mensagens em ordens distintas.

A fim de garantir a ordem causal, as mensagens têm um _vector clock_ associado, sendo o identificador do servidor definido num ficheiro de configuração, evitando assim a utilização de _strings_ probabilisticamente únicas. A partir daí podemos inferir se o servidor já recebeu todas as mensagens que estavam no passado causal de _Y_, em caso afirmativo _Y_ pode ser entregue, senão é necessário continuar a aguardar.

Convém relembrar que a dependência entre mensagens é estabelecida dentro do tópico, como tal o servidor de _chat_ possui um _vector clock_ por tópico, caso contrário um _clock_ para todos os tópicos estaria sempre a crescer, e originaria uma dependência circular, podendo levar a _deadlocks_ durante a entrega.

== Utilizadores Online

A entrada e saída de utilizadores pode ocorrer em qualquer servidor, como tal os mesmos devem partilhar o seu estado a fim de convergirem para um certo valor, perante tal requisito a utilização de _CRDTs Operation Based_ torna-se bastante relevante, dado que as operações de _add_ e _remove_ podem ser propagadas e o algoritmo resolve os conflitos resultantes de eventos concorrentes.

A estrutura de dados que mais se adequa ao registo dos utilizadores _online_ é um _Map_ de _ORSet_ por tópico, pois como anteriormente, os eventos de entrada e saída de utilizadores são ao nível do tópico.

== Registo de Logs

Todas as mensagens enviadas e recebidas pelo servidor de _chat_ devem ser armazenadas num _log_ cuja única operação permitida é _append_, além disso todos os servidores devem convergir para o mesmo registo, sendo por isso definida uma ordem total na escrita de _logs_.

A comunicação entre servidores garante apenas a ordem causal, daí que seja preciso definir uma ordem total a partir duma parcial, neste caso podemos preservar a ordem causal e utilizar o identificador do servidor que enviou a mensagem como critério de desempate.

Seja como for, o grande desafio reside em saber quando podemos escrever no _log_, pois se um servidor não estiver a comunicar, o seu _clock_ nunca será incrementado e podemos receber uma mensagem concorrente cuja localização no _log_ antecede todas as outras.

Perante tal questão, o grupo decidiu que após a receção de uma mensagem, todas as que lhe são concorrentes chegam num intervalo máximo de $500$ milissegundos, daí que as tentativas de escrita no _log_ ocorram nesse período de tempo. 

== Arquitetura

Para servir os propósitos do servidor de _chat_, foram definidos vários componentes que garantem a entrega causal de mensagens, registo de _logs_, publicação de mensagens e comunicação com o cliente, sendo que todos eles comunicam através de _queues_ e _sockets_ com endereços `inproc` ou `tcp`. 

#image_block(
  imagem: image("/images/chat_arch.png"),
  caption: [Arquitetura do Servidor de _Chat_]
)

=== Comunicações Internas

O _socket_ do tipo `Sub` é utilizado para receber mensagens de outros servidores de _chat_, mas antes disso o servidor de agregação comunica a alocação de um tópico através do `Rep`, sendo essa informação passada ao `Pub` que publica a mensagem num tópico interno, deste modo somente o `Sub` local recebe a lista dos servidores de _chat_ aos quais deve conectar-se.

=== Comunicações Externas

Os restantes _sockets_ utilizados expõem o endereço ao qual dão _bind_, a fim dos demais componentes do sistema poderem usufruir do serviço, seja no caso dos clientes para enviar mensagens, ou o servidor de agregação que necessita de conhecer o estado interno.

==== Proxy

Para responder a pedidos de informação, é utilizado um _proxy_ `Router-Dealer` que esconde a execução de vários _workers_, deste modo é possível responder a diversos pedidos em simultâneo, tantos quanto o limite de _workers_ definido no ficheiro de configuração.

==== Pull

Antes de enviar mensagens para um tópico, o cliente indica que pretende aceder ao mesmo, imediatamente a seguir são aplicadas as operações de _preprare_ sobre o _ORSet_ correspondente e a operação é propagada, depois disso o cliente pode finalmente publicar mensagens no tópico, sendo que no final deve indicar a sua intenção de sair para que o servidor atualize o _ORSet_. 

==== Pub-Sub

A troca de mensagens entre servidores de _chat_ é realiza através do paradigma `Pub-Sub`, assim os servidores associados ao tópico _X_ subscrevem esse mesmo tópico, e consequentemente a difusão de mensagens ocorre automaticamente, pois uma publicação no tópico _X_ será recebida por todos aqueles que o subscreveram.

==== gRPC

A obtenção dos _logs_ associados ao tópico geralmente envolve o envio de grandes quantidades de informação, nesse sentido o grupo optou por combinar as _streams_ assíncronas do _ReactiveX_ com as conecções eficientes do _gRPC_.

Posto isto, um pedido de _logs_ origina a emissão de eventos com _back pressure_, o que evita a saturação do _link_ do cliente, especialmente quando a capacidade do mesmo é bastante reduzida, além disso o pedido de _logs_ pode ser parcial (visando apenas os últimos _X_ registos) e filtrado pelo cliente que originalmente enviou a mensagem.