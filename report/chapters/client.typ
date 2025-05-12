#import "/utils.typ": image_block 

= Cliente

Este componente tem por objetivo fornecer uma interface de utilização, sendo toda a lógica de comunicações abstraída do utilizador final, além disso convém realçar que a diversidade de ligações é mais vasta neste caso, sendo utilizados _sockets_ _TCP_, _ZeroMQ_ e _stubs_ do _gRPC_. 

== Arquitetura

Antes de entrar num determinado tópico, o cliente necessita de conhecer os endereços dos servidores de _chat_ que lhe estão associados, para isso recorre a servidor de pesquisa que devolve lista vazia no caso do tópico não existir.

Perante tal situação, o cliente deve contactar um servidor de agregação para encontrar os servidores de _chat_ mais apropriados, sendo que no final a _DHT_ é atualizada e novamente lida pelo cliente, a partir daí é aleatoriamente escolhido um endereço ao qual este irá conectar-se para publicar mensagens.

#image_block(
  imagem: image("../images/client_arch.png"),
  caption: [Arquitetura do Cliente]
)

=== Comunicações Internas

No momento em que o cliente acede a um tópico, essa informação é transmitida ao servidor de _chat_ pelo `Push`, consequentemente é necessário que o `Sub` subscreva esse mesmo tópico, no entanto o _worker_ em questão está bloqueado a ler do _socket_, daí que seja necessário criar um tópico interno para informar o _worker_ acerca na entrada/saída no tópico. 

==== Sinais

Tal como descrito anteriormente, a utilização do _ZeroMQ_ dificulta a identificação de mortes, daí que o grupo tenha implementado um sistema de _heartbeats_ entre o cliente e o servidor de _chat_, deste modo quando o `Sub` der _timeout_ é emitido um sinal que inicia uma tentativa de comunicação com outro servidor de _chat_ do mesmo tópico.

Além disso, os sinais tornam-se bastante úteis para comunicar as ações realizadas na _GUI_, ou seja, todos os eventos são transformados em sinais que posteriormente são inseridos numa _queue_ e interpretados por um _handler_ que gere parte das comunicações. 

=== Comunicações Externas

De forma semelhante ao servidor de agregação, o cliente faz uso de um `Req` para recolher informações do servidor de _chat_, enquanto o outro `Req` é aplicado para fazer pedidos de agregação, daí a razão dos servidores iniciarem as agregações sequencialmente.

Além disso, numa perspetiva de comunicação assíncrona, o `Push` é requerido durante a publicação de mensagens sobre o tópico, enquanto o `Sub` recebe as respetivas publicações e deteta mortes por _timeout_, de notar que um cliente recebe todas as mensagens, incluindo aquelas enviadas por si próprio.

==== gRPC

A partir do momento em que o servidor disponibiliza os _logs_ através do _gRPC_, o cliente é obrigado a seguir o mesmo padrão, consequentemente é emitido um sinal de _logs request_ que origina a invocação do _RPC_ `getLogs` ou `getUserLogs`, dependendo obviamente da intenção do utilizador.

Tendo em consideração que o _RPC_ devolve uma _stream_, as mensagens são recebidas conforme a política de _back pressure_ do servidor de _chat_, assim sendo a visualização das mesmas ocorre em tempo real, não sendo necessário esperar que o _RPC_ termine.

== Interface de Utilização

Com o objetivo de facilitar a utilização do programa e organizar a apresentação de informação, o grupo optou por desenvolver uma _GUI_ bastante simplista, nela o utilizador pode visualizar, em tempo real, as mensagens publicadas, bem como os demais dados fornecidos pelo servidor de _chat_.

#grid(
    columns: 2,
    gutter: 5mm,
    figure(
      image("../images/cli1.png"),
      caption: [Interface do cliente _cli1_]
    ),
    figure(
      image("../images/cli2.png"),
      caption: [Interface do cliente _cli2_]
    )
)

Neste exemplo de execução, ambos os clientes subscreveram o tópico _uminho_, tendo sido enviadas duas mensagens por cada participante (_InBox_), por outro lado, o utilizador _cli1_ recolheu os utilizadores que estavam _online_, enquanto o _cli2_ solicitou todas as mensagens enviadas por _cli1_ (_Info_). Na última caixa de _texto_ são apresentados os _logs_ internos do cliente, algo bastante útil durante a fase de deteção de erros (_backtracking_).