# Impacto após a Parte 1 — Gama, Delta e consultas

## O que foi apenas adicionado

- contrato JSON achatado do Gama;
- agrupamento das linhas pelo número do pedido;
- mapper de timestamp Unix, situação numérica, centavos e fator de conversão;
- endpoint `POST /api/v1/imports/gama`;
- arquivo de amostra e testes específicos;
- migration V3 para metadados de auditoria da conversão.
- contratos JSON independentes de pedidos e itens do Delta;
- assembler para correlacionar as duas fontes pelo número do pedido;
- endpoint `POST /api/v1/imports/delta` e rejeição explícita de itens órfãos;
- arquivos de amostra e testes de importação, reenvio, consulta e conferência Delta;
- snapshots materializados para pedidos e limite temporal `snapshotAt` para o relatório;
- listagem detalhada e filtro por resultado no relatório de conferências.

Os casos de uso de consulta, conferência e relatório não receberam condições específicas para Gama ou Delta. Depois da normalização, eles trabalham com os mesmos objetos usados por Alfa e Beta.

## O que precisou ser modificado

`PurchaseOrderItem` ganhou três campos opcionais:

- unidade de compra original;
- fator de conversão;
- preço original na unidade de compra.

Depois, ganhou também a data opcional do item, necessária porque o Delta informa datas independentes no cabeçalho e nas linhas. Pedidos passaram a aceitar uma lista vazia de itens para representar corretamente um cabeçalho Delta que ainda não possui linhas na segunda fonte.

Esses campos não participam das regras centrais. Eles preservam a transformação para auditoria e aparecem como `sourceDetails` no detalhe do pedido. As migrations V3 e V4 criaram colunas e índices de forma aditiva, portanto pedidos Alfa e Beta continuam válidos. As migrations V5 e V6 adicionaram suporte e índices para as consultas, e a V7 criou os snapshots materializados de pedidos.

## Decisões específicas

- Como o Gama não informa moeda, foi assumido `BRL`, documentado como premissa.
- O timestamp é interpretado em UTC antes da conversão para `LocalDate`.
- As notas sempre usam unidades; portanto, quantidades de caixas são multiplicadas pelo fator.
- O preço por caixa é convertido para preço unitário dividindo-o pelo fator.
- Divisões não exatas usam escala de seis casas e `HALF_UP`; a conferência continua arredondando o total final para duas casas.
- Os dados repetidos de cabeçalho precisam ser idênticos entre as linhas do mesmo pedido.

### Delta

- A integração recebe `ordersFile` e `itemsFile` na mesma requisição para representar as duas APIs sem depender de serviços externos durante a avaliação.
- A associação usa o número do pedido; itens órfãos são reportados e não descartados silenciosamente.
- Pedidos sem itens permanecem consultáveis, pois a ausência pode refletir defasagem entre as fontes.
- Cabeçalhos ou linhas duplicados são erros de consistência e causam rollback.

### Paginação e relatório

- `snapshotId` identifica a sequência materializada da listagem de pedidos e é reaproveitado nas páginas seguintes.
- O snapshot guarda também os resumos, protegendo a varredura contra reimportações que alterem campos filtrados; sua retenção é de 24 horas.
- No relatório, que contém registros imutáveis, `snapshotAt` fixa o limite temporal.
- O identificador atua como desempate da ordenação para produzir sequências determinísticas.
- O relatório separa o resumo consolidado da lista paginada, mas ambos respeitam os mesmos filtros.

## Evidência de extensibilidade

Gama e Delta entraram principalmente como novos adaptadores. Não foi necessário criar variações por cliente para:

- busca e filtros de pedidos;
- identificação `source + number`;
- associação dos itens da nota;
- regras de quantidade e preço;
- persistência de conferências;
- cálculo do relatório.

Se outro cliente enviasse XML, seria criado um novo parser e mapper para o mesmo modelo normalizado. Mudanças no domínio somente seriam necessárias se ele trouxesse um conceito de negócio ainda não representado, e não apenas outro formato de transporte.
