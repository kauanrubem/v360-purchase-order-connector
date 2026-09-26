# Modelo normalizado

## Objetivo

Este modelo é o contrato interno da aplicação. Os formatos de Alfa, Beta, Gama e Delta são convertidos para ele na fronteira de importação. Casos de uso de consulta, conferência e relatório não devem depender de campos ou vocabulários particulares dos clientes.

## Agregados

### PurchaseOrder

Raiz do agregado de pedido de compra.

| Campo | Tipo Java | Obrigatório | Regra |
|---|---|---:|---|
| `id` | `UUID` | sim | Identificador interno gerado pela aplicação. |
| `source` | `ClientSource` | sim | Cliente de origem. |
| `number` | `String` | sim | Número preservado como texto. |
| `createdAt` | `LocalDate` | sim | Data normalizada. |
| `status` | `PurchaseOrderStatus` | sim | `OPEN`, `CLOSED` ou `BLOCKED`. |
| `currency` | `Currency` | sim | Código ISO 4217, inicialmente `BRL`. |
| `vendor` | `Vendor` | sim | Fornecedor do pedido. |
| `items` | `List<PurchaseOrderItem>` | sim | Pode ser vazia no Delta, cujas fontes de pedido e item são independentes. |
| `importedAt` | `Instant` | sim | Momento da última importação aceita. |

Restrição de unicidade: `source + number`.

### Vendor

| Campo | Tipo Java | Obrigatório | Regra |
|---|---|---:|---|
| `taxId` | `String` | sim | CNPJ normalizado com 14 dígitos. |
| `name` | `String` | sim | Razão social não vazia. |

### PurchaseOrderItem

| Campo | Tipo Java | Obrigatório | Regra |
|---|---|---:|---|
| `line` | `String` | sim | Linha preservada como texto para não assumir numeração inteira. |
| `materialCode` | `String` | sim | Código não vazio. |
| `description` | `String` | sim | Descrição do material. |
| `unitOfMeasure` | `String` | sim | Unidade usada na conferência. |
| `quantityOrdered` | `BigDecimal` | sim | Maior ou igual a zero. |
| `quantityReceived` | `BigDecimal` | sim | Entre zero e a quantidade pedida. |
| `unitPrice` | `BigDecimal` | sim | Maior ou igual a zero. |
| `sourceDetails` | `SourceItemDetails` | não | Dados necessários para auditar conversões. |

`quantityRemaining` é calculado e não armazenado no domínio:

```text
quantityRemaining = quantityOrdered - quantityReceived
```

A linha é única dentro do pedido. O código do material não precisa ser único, pois o mesmo material pode aparecer em condições comerciais distintas.

### SourceItemDetails

Metadados opcionais da representação de origem, úteis para auditar conversões do Gama e datas próprias dos itens Delta.

| Campo | Tipo Java | Obrigatório |
|---|---|---:|
| `purchaseUnit` | `String` | não |
| `conversionFactor` | `BigDecimal` | não |
| `purchaseUnitPrice` | `BigDecimal` | não |
| `createdAt` | `LocalDate` | não |

Os campos não participam da conferência; servem para rastreabilidade.

### InvoiceValidation

Registro imutável de uma tentativa executável de conferência.

| Campo | Tipo Java | Obrigatório |
|---|---|---:|
| `id` | `UUID` | sim |
| `purchaseOrderId` | `UUID` | não, quando não encontrado |
| `source` | `ClientSource` | sim |
| `purchaseOrderNumber` | `String` | sim |
| `vendorTaxId` | `String` | sim |
| `invoiceItems` | `List<InvoiceItemSnapshot>` | sim |
| `status` | `ValidationStatus` | sim |
| `divergences` | `List<Divergence>` | sim |
| `validatedAt` | `Instant` | sim |

### InvoiceItemSnapshot

| Campo | Tipo Java | Obrigatório |
|---|---|---:|
| `purchaseOrderLine` | `String` | não |
| `materialCode` | `String` | sim |
| `quantity` | `BigDecimal` | sim |
| `totalAmount` | `BigDecimal` | sim |

### Divergence

| Campo | Tipo Java | Obrigatório |
|---|---|---:|
| `type` | `DivergenceType` | sim |
| `message` | `String` | sim |
| `invoiceItemIndex` | `Integer` | não |
| `purchaseOrderLine` | `String` | não |
| `materialCode` | `String` | não |
| `expectedValue` | `String` | não |
| `actualValue` | `String` | não |

Os valores esperado e recebido são textos porque uma divergência pode comparar número, situação, material ou CNPJ. A estrutura poderá evoluir para detalhes tipados se surgir uma necessidade concreta.

## Enumerações

```text
ClientSource: ALFA, BETA, GAMA, DELTA
PurchaseOrderStatus: OPEN, CLOSED, BLOCKED
ValidationStatus: APPROVED, REJECTED
```

Os tipos iniciais de divergência estão definidos em `business-rules.md` e reproduzidos no contrato OpenAPI.

## Identificadores expostos pela API

Consultas e conferências usarão `source + purchaseOrderNumber` na URL. O UUID interno continuará presente nas respostas para rastreabilidade, mas o consumidor não precisará conhecê-lo para localizar um pedido.

Exemplo:

```text
GET /api/v1/purchase-orders/ALFA/4500001234
```

## Limites do modelo

- A moeda é definida no nível do pedido.
- Cada pedido possui um único fornecedor.
- A conferência é feita contra um único pedido.
- Tributos, descontos, frete e tolerâncias contratuais específicas não fazem parte do escopo inicial.
- O modelo externo dos clientes nunca será usado diretamente como resposta da API normalizada.
