# Regras de negócio e premissas

## 1. Objetivo do serviço

O serviço atua como uma camada anticorrupção entre os formatos particulares dos clientes e a plataforma V360. Cada integração conhece o formato de sua origem, mas as consultas, conferências e relatórios trabalham exclusivamente com um modelo normalizado.

O serviço precisa atender a três capacidades:

1. importar e consultar pedidos de compra dos clientes;
2. conferir uma nota fiscal contra um pedido;
3. registrar as conferências e consolidar seus resultados em relatório.

## 2. Glossário

- **Pedido de compra:** acordo emitido pelo comprador que registra fornecedor, materiais, quantidades e preços.
- **Item do pedido:** linha contendo um material e as condições comerciais acordadas.
- **Nota fiscal:** documento enviado pelo fornecedor com os materiais entregues e cobrados.
- **Quantidade recebida:** quantidade que o sistema do cliente informa como já recebida.
- **Saldo a receber:** quantidade pedida menos a quantidade recebida.
- **Conferência:** comparação entre uma nota fiscal e o pedido informado, sem alteração automática do pedido.
- **Divergência:** inconsistência estruturada encontrada durante uma conferência.

## 3. Identidade e normalização

### 3.1 Identidade do pedido

Um pedido é identificado pela composição de `cliente de origem + número do pedido`.

O número isolado não será considerado globalmente único, pois clientes diferentes podem utilizar a mesma numeração.

### 3.2 Fornecedor

- O CNPJ será normalizado para seus 14 dígitos, removendo pontuação e espaços.
- Um CNPJ com quantidade diferente de 14 dígitos será rejeitado durante a importação ou conferência.
- O CNPJ, e não a razão social, será utilizado para comparar fornecedores.
- A razão social será preservada para exibição, mas diferenças de grafia não provocarão divergência.

### 3.3 Datas

- Datas serão normalizadas como datas civis no formato ISO `YYYY-MM-DD`.
- Alfa: leitura direta do formato ISO.
- Beta: conversão de `dd/MM/yyyy` para o formato normalizado.
- Gama: conversão do timestamp Unix em segundos usando UTC antes da extração da data.
- Datas inválidas provocarão rejeição da carga; não serão corrigidas silenciosamente.

### 3.4 Situação do pedido

O vocabulário interno terá três valores:

| Situação normalizada | Alfa | Beta | Gama |
|---|---|---|---|
| `OPEN` | `open` | `EM ABERTO` | `1` |
| `CLOSED` | `closed` | `ENCERRADO` | `2` |
| `BLOCKED` | `blocked` | `BLOQUEADO` | `3` |

Valores não reconhecidos serão tratados como erro de importação. Não haverá situação padrão implícita.

### 3.5 Valores monetários

- Valores serão tratados com precisão decimal, nunca com ponto flutuante binário.
- A moeda pertence ao pedido e deve ser preservada.
- A primeira versão aceitará somente pedidos cuja moeda esteja presente e seja consistente em todos os itens.
- O valor esperado de uma linha da nota será `quantidade faturada × preço unitário normalizado`.
- Comparações monetárias serão feitas após arredondamento para duas casas decimais, usando `HALF_UP`.
- Será aceita diferença absoluta máxima de R$ 0,01 entre o valor esperado e o informado, para acomodar arredondamentos.

Embora os exemplos usem BRL, a moeda será mantida no modelo para não assumir que todos os clientes operam exclusivamente em reais. A tolerância monetária poderá ser evoluída por moeda em uma versão futura.

### 3.6 Quantidades

- Quantidades serão representadas com precisão decimal para atender também unidades fracionárias, como quilogramas.
- Quantidades pedidas e recebidas não podem ser negativas.
- A quantidade recebida não pode ser maior que a quantidade pedida na carga do pedido.
- O saldo será calculado como `quantidade pedida - quantidade recebida`.
- Uma quantidade de nota deve ser maior que zero.

### 3.7 Unidade e fator de conversão do Gama

As notas fiscais sempre informam quantidades em unidades individuais. Por isso, os itens do Gama serão normalizados da seguinte maneira:

```text
quantidade pedida normalizada = quantidade pedida na unidade de compra × fator de conversão
quantidade recebida normalizada = quantidade recebida na unidade de compra × fator de conversão
preço unitário normalizado = preço da unidade de compra ÷ fator de conversão
```

Exemplo: dez caixas com doze unidades e preço de R$ 1.200,00 por caixa resultam em 120 unidades ao preço normalizado de R$ 100,00 por unidade.

- O fator de conversão deve ser maior que zero.
- A unidade de compra e o fator original serão preservados como dados de origem para auditoria.
- Quando o fator for `1`, as quantidades e o preço permanecem equivalentes aos valores recebidos.
- Quando a divisão do preço pelo fator não for exata, o preço normalizado terá seis casas decimais com arredondamento `HALF_UP`.
- Como o formato do Gama não informa moeda, a integração assume `BRL` e registra essa premissa explicitamente.

## 4. Importação dos pedidos

### 4.1 Validação da carga

Uma carga inválida não poderá produzir um pedido parcialmente persistido. Todos os itens de um pedido devem ser validados antes de sua gravação.

Pedidos válidos e inválidos de uma mesma carga poderão ser tratados independentemente, desde que o resultado da importação informe claramente quais pedidos foram aceitos e quais foram rejeitados. Essa granularidade será confirmada no desenho do contrato da API.

### 4.2 Reenvio e atualização

O reenvio de um pedido será idempotente em relação a `cliente + número`:

- se o pedido ainda não existir, será criado;
- se já existir, seus dados e itens serão atualizados para refletir a versão mais recente enviada pelo cliente;
- o reenvio não criará um segundo pedido com a mesma identidade;
- o histórico das conferências realizadas será preservado.

A origem é a autoridade sobre as quantidades já recebidas. A aplicação não somará recebimentos durante uma conferência de nota.

### 4.3 Particularidades por cliente

#### Alfa Energia

- O JSON já contém pedidos com itens aninhados.
- Datas, situações e valores serão validados e convertidos para os tipos internos.

#### Beta Alimentos

- A importação exige os arquivos de cabeçalhos e itens em conjunto.
- A associação será feita pelo número do pedido.
- Uma linha de item sem cabeçalho correspondente será rejeitada.
- Um cabeçalho sem itens será rejeitado, pois não constitui um pedido útil para conferência.
- O separador é ponto e vírgula.
- Quantidades e preços seguirão o padrão brasileiro, com ponto para milhar e vírgula para decimal.

#### Gama Logística

- As linhas serão agrupadas pelo número do pedido.
- Campos de cabeçalho repetidos nas linhas de um mesmo pedido devem ser consistentes.
- Divergências de fornecedor, data ou situação entre linhas do mesmo pedido provocarão rejeição do pedido.
- Timestamp, centavos, códigos de situação e fatores de conversão serão normalizados pelo adaptador do Gama.

## 5. Consulta de pedidos

A listagem deverá admitir, no mínimo, os seguintes filtros combináveis:

- cliente de origem;
- CNPJ do fornecedor;
- situação;
- somente pedidos com algum saldo a receber.

Um pedido possui saldo a receber quando pelo menos um de seus itens tem saldo maior que zero.

A consulta detalhada exibirá, para cada item:

- quantidade pedida;
- quantidade recebida;
- saldo a receber;
- preço unitário normalizado;
- unidade normalizada.

A listagem será paginada para evitar respostas sem limite quando houver grandes volumes.

## 6. Conferência de nota fiscal

### 6.1 Comportamento geral

- A conferência é uma operação de validação e auditoria.
- Ela não altera a quantidade recebida do pedido.
- Todas as divergências detectáveis serão retornadas de uma só vez, em vez de interromper na primeira falha.
- Toda tentativa de conferência com pedido existente será registrada para o relatório, aprovada ou rejeitada.
- Requisições estruturalmente inválidas, que não representam uma conferência executável, retornarão erro de validação da API e não entrarão no relatório.

### 6.2 Condições para aprovação

Uma nota será aprovada somente quando:

1. o pedido existir;
2. o pedido estiver aberto;
3. o CNPJ do fornecedor corresponder ao fornecedor do pedido;
4. todos os itens puderem ser associados sem ambiguidade a itens do pedido;
5. todas as quantidades forem positivas e couberem no saldo dos respectivos itens;
6. todos os valores totais corresponderem às quantidades e preços acordados, dentro da tolerância definida.

### 6.3 Associação dos itens

O identificador preferencial será a linha do pedido quando ela for informada. O código do material também será validado.

Quando a linha não for informada:

- se existir exatamente um item com o material, ele será utilizado;
- se nenhum item possuir o material, haverá divergência de material inexistente;
- se houver mais de um item com o mesmo material, haverá divergência de associação ambígua.

Itens repetidos na nota que apontem para a mesma linha do pedido serão agregados para validar quantidade e valor total. Isso impede que duas linhas individualmente válidas ultrapassem o saldo quando consideradas em conjunto.

### 6.4 Tipos iniciais de divergência

| Código | Significado |
|---|---|
| `PURCHASE_ORDER_NOT_FOUND` | O pedido informado não existe. |
| `PURCHASE_ORDER_CLOSED` | O pedido está encerrado. |
| `PURCHASE_ORDER_BLOCKED` | O pedido está bloqueado. |
| `VENDOR_MISMATCH` | O CNPJ da nota difere do CNPJ do pedido. |
| `MATERIAL_NOT_FOUND` | O material da nota não existe no pedido. |
| `PURCHASE_ORDER_ITEM_NOT_FOUND` | A linha do pedido informada não existe. |
| `MATERIAL_MISMATCH` | A linha existe, mas corresponde a outro material. |
| `AMBIGUOUS_MATERIAL` | O material aparece em mais de uma linha e a nota não informa qual deve ser usada. |
| `INVALID_QUANTITY` | A quantidade informada é zero ou negativa. |
| `QUANTITY_EXCEEDS_REMAINING` | A quantidade total faturada ultrapassa o saldo da linha. |
| `PRICE_MISMATCH` | O valor total está fora da tolerância monetária. |

Cada divergência deverá trazer dados suficientes para apresentação direta ao usuário, incluindo código, mensagem, item da nota, valores esperado e recebido quando aplicáveis.

## 7. Histórico e relatório

Cada registro de conferência deverá preservar pelo menos:

- identificador da conferência;
- data e hora;
- cliente e pedido;
- resultado `APPROVED` ou `REJECTED`;
- divergências encontradas;
- dados submetidos da nota necessários para auditoria.

O relatório deverá informar:

- total de conferências;
- total aprovado;
- total rejeitado;
- quantidade de ocorrências por tipo de divergência.

Uma conferência com várias divergências conta uma única vez como rejeitada, mas contribui uma ocorrência para cada tipo de divergência encontrado.

## 8. Decisões conscientes e limitações iniciais

### Conferência não realiza recebimento

O enunciado não declara que uma aprovação representa a entrada definitiva da mercadoria. Alterar o saldo durante a conferência também criaria risco de duplicidade em reenvios. Por isso, somente uma nova carga do sistema do cliente atualizará a quantidade recebida.

### Identificação da nota e idempotência

O enunciado não fornece número ou chave de acesso da nota. Consequentemente, nesta primeira versão cada requisição executável gera uma nova conferência. Uma evolução natural seria exigir um identificador único da nota e tornar a conferência idempotente.

### Moeda da nota

O pedido contém moeda, mas o formato mínimo da nota descrito no desafio não contém. Inicialmente, os valores da nota serão interpretados na moeda do pedido. Uma evolução poderá tornar a moeda obrigatória na requisição e validar sua correspondência.

### Pedidos inexistentes no relatório

Uma tentativa contra pedido inexistente pode ser útil operacionalmente, mas não oferece cliente de origem confiável se ele não fizer parte da identificação da requisição. O contrato da API deverá exigir a origem junto ao número, permitindo registrar também esse resultado sem ambiguidade.

## 9. Pontos para validação durante a implementação

Estas decisões estão registradas antes do código e deverão ser revisitadas se os testes ou o desenho do contrato revelarem inconsistências:

- granularidade transacional de cargas com vários pedidos;
- representação da unidade normalizada para itens originalmente medidos em `KG`;
- política de arredondamento quando a divisão pelo fator de conversão produzir dízima;
- filtros adicionais do relatório, como período e cliente;
- dados completos que devem ser preservados para auditoria sem duplicação excessiva.
