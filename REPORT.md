# Entrega 3 Projeto SD

Para alcançar o objetivo desta entrega adoptámos uma implementação em que, a partir do momento em que existem dois servidores disponíveis, é feita a propagação de estado de um servidor para outro periodicamente (deste que o gossip esteja ativo para os servidores). Quanto ao tempo entre propagações decidimos que para o servidor P esta operações aconteceria a cada 15 segundos e para o servidor S, 30 segundos. Esta disparidade nos tempos deve-se ao facto de, apesar de nesta entrega o servidor S já suportar inscrições, existirem mais operações que alteram o estado no servidor P do que no servidor S.

Quanto à solução adoptada para garantir a coerência eventual, decidimos usar vector clocks de forma a conseguirmos ter uma ideia de quando um servidor se encontrava desatualizado em relação ao outro. Cada School mantém um vector clock (uma vez que cada servidor tem uma School e assim permite-nos uma maior facilidade no acesso ao clock através de outras classes) que é inicializado a zeros ([0,0] em que número no índice 0 corresponde ao servidor P o outro ao servidor S). A cada operação que de facto altera o estado do ClassState o vector clock do servidor que realizou essa operação é incrementado. 

Cada PropagateStateRequest passou a incluir o clock do outro servidor de forma a quando um servidor recebe um pedido destes, este conseguir estabelecer uma relação de 'happened before' tendo em conta também o seu próprio clock. Se, ao receber um pedido, o servidor que recebeu o pedido perceber que está desatualizado, simplesmente atualiza o seu ClassState para o ClassState que recebeu no pedido e atualiza também o seu clock para o clock recebido. Se o servidor perceber que recebeu um pedido desatualizado não faz nada em relação ao seu ClassState. No caso em que aconteceram alterações em ambos os servidores e, ao receber um pedido, um servidor não consegue perceber quem tem o estado mais atualizado, é feito "merge" de ambos os ClassStates. De forma a manter uma certa coerência entre casos destes adoptámos as seguintes "merge policies" que nos permitem resolver estas situações sempre de forma idêntica:
    1. No caso de as capacidades da turma diferirem, ficamos com a **maior das capacidades**;
    2. No caso de o enrollment status diferir, o estado fica a **false**;
    3. No caso de a lista de estudantes "discarded" diferir:
       - se um estudante estiver na lista de estudantes inscritos que tínhamos e na lista de discarded que recebemos, esse estudante é **mantido na lista de discarded**;
       - se o estudante apenas estiver na lista de discarded é **mantido nessa lista** também;
    4. No caso de a lista de estudantes inscritos diferir:
       - se a capacidade assim o permitir **ficamos com todos os estudantes que estão em ambas as listas**;
       - caso a turma fique cheia, **escolhemos aleatoriamente os estudantes que passam para a lista "discarded"**;

Nos casos em que é feito merge dos ClassStates, o PropagateStateResponse inclui também o ClassState que ficou decidido como final de forma a ambos os servidores ficarem com o mesmo ClassState. É também incluído o clock na resposta de forma a ambos ficarem também com o mesmo clock.
