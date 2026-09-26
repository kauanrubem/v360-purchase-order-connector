# Uso de inteligência artificial

## Ferramentas utilizadas

Utilizei o Codex, da OpenAI, como apoio durante o desenvolvimento. A ferramenta foi usada para:

- decompor o enunciado em regras de negócio e etapas de entrega;
- discutir ambiguidades, como baixa de saldo, tolerância monetária e materiais repetidos;
- propor o contrato inicial da API e a organização dos pacotes;
- gerar rascunhos de código, migrations, testes e documentação;
- revisar a extensibilidade ao incorporar as duas fontes Delta e a paginação estável;
- executar os testes, interpretar falhas e revisar a consistência entre implementação e documentação.

O código sugerido não foi aceito automaticamente. As decisões foram confrontadas com o enunciado, compilação, testes automatizados e comportamento esperado da API.

## Exemplo que funcionou bem

Prompt resumido:

> Analise o desafio e proponha uma ordem de implementação que preserve a Parte 1 antes da chegada do Gama, deixando explícitas as regras ambíguas que precisam ser documentadas.

A resposta ajudou a trocar uma construção horizontal de todas as camadas por fatias verticais. Primeiro foi implementado o fluxo completo do Alfa e depois o Beta reutilizou o mesmo domínio. Também foram aproveitadas as recomendações de identificar pedidos por cliente e número, usar `BigDecimal` e não alterar o saldo durante a conferência.

## Exemplo de erro e correção

Ao adicionar os testes de conferência, o código inicialmente capturava dentro de uma lambda o índice mutável de um laço. A compilação Java rejeitou a implementação. O trecho foi reescrito com controle explícito, compilado novamente e coberto pelos testes.

Outro problema apareceu na suíte completa: históricos de conferência de uma classe de teste impediam a exclusão dos pedidos na classe seguinte por causa da chave estrangeira. A falha foi identificada pelo erro de integridade do H2. A preparação dos testes passou a apagar primeiro as conferências e depois os pedidos, respeitando a mesma integridade existente no PostgreSQL.

Esses episódios reforçaram que uma resposta plausível da IA não substitui compilação, execução e análise do modelo de dados.

## Como validei as respostas

- compilei o projeto com Java 21 e Maven;
- executei testes HTTP integrados com Spring Boot e MockMvc;
- testei importação, normalização, reenvio, filtros, conferência e relatório;
- mantive constraints também no banco de dados;
- executei `git diff --check` para detectar problemas de formatação;
- registrei decisões e limitações antes de expandir o escopo.

Na Parte 2, os testes também validaram numericamente a conversão do Gama: 10 caixas de 12 unidades foram normalizadas para 120 unidades, com 24 recebidas e preço de R$ 100,00 por unidade. Para o Delta, validaram a correlação entre as duas fontes, item órfão, pedido sem itens, reimportação e reutilização das regras de conferência. A paginação foi testada para garantir estabilidade com o mesmo `snapshotAt`.

## Como garanto que entendo o código

Consigo explicar o caminho completo de uma requisição: controller, DTO externo, normalização, agregado, repositório, resposta e persistência. Também consigo justificar as regras adotadas, reproduzir os testes, alterar os adaptadores e explicar os trade-offs registrados em `docs/architecture.md` e `docs/business-rules.md`.
