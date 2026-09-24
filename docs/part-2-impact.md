# Impacto da Parte 2 — Cliente Gama

## O que foi apenas adicionado

- contrato JSON achatado do Gama;
- agrupamento das linhas pelo número do pedido;
- mapper de timestamp Unix, situação numérica, centavos e fator de conversão;
- endpoint `POST /api/v1/imports/gama`;
- arquivo de amostra e testes específicos;
- migration V3 para metadados de auditoria da conversão.

Os casos de uso de consulta, conferência e relatório não receberam condições específicas para o Gama. Depois da normalização, eles trabalham com os mesmos objetos usados por Alfa e Beta.

## O que precisou ser modificado

`PurchaseOrderItem` ganhou três campos opcionais:

- unidade de compra original;
- fator de conversão;
- preço original na unidade de compra.

Esses campos não participam das regras centrais. Eles preservam a transformação para auditoria e aparecem como `sourceDetails` no detalhe do pedido. Uma migration aditiva e retrocompatível criou as colunas como opcionais, portanto pedidos Alfa e Beta continuam válidos.

## Decisões específicas

- Como o Gama não informa moeda, foi assumido `BRL`, documentado como premissa.
- O timestamp é interpretado em UTC antes da conversão para `LocalDate`.
- As notas sempre usam unidades; portanto, quantidades de caixas são multiplicadas pelo fator.
- O preço por caixa é convertido para preço unitário dividindo-o pelo fator.
- Divisões não exatas usam escala de seis casas e `HALF_UP`; a conferência continua arredondando o total final para duas casas.
- Os dados repetidos de cabeçalho precisam ser idênticos entre as linhas do mesmo pedido.

## Evidência de extensibilidade

O Gama entrou principalmente como um novo adaptador. Não foi necessário modificar:

- busca e filtros de pedidos;
- identificação `source + number`;
- associação dos itens da nota;
- regras de quantidade e preço;
- persistência de conferências;
- cálculo do relatório.

Se um quarto cliente enviasse XML, seria criado um novo parser e mapper para o mesmo modelo normalizado. Mudanças no domínio somente seriam necessárias se o novo cliente trouxesse um conceito de negócio ainda não representado, e não apenas outro formato de transporte.

