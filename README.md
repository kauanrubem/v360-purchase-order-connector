# V360 Purchase Order Connector

Serviço backend responsável por receber pedidos de compra em formatos específicos de cada cliente e disponibilizá-los em um contrato normalizado para consulta e conferência de notas fiscais.

O projeto será desenvolvido em duas partes:

- **Parte 1:** integrações com Alfa Energia e Beta Alimentos;
- **Parte 2:** inclusão da Gama Logística e da Delta, além de evolução da paginação e do relatório, após a marcação da Parte 1.

## Funcionalidades

- importação JSON do Cliente Alfa;
- importação dos dois CSVs do Cliente Beta;
- importação JSON achatada do Cliente Gama, com conversão de caixas para unidades;
- importação Delta por dois arquivos JSON independentes, correlacionando pedidos e itens;
- reimportação idempotente por cliente e número do pedido;
- consulta paginada com filtros combináveis;
- detalhe com quantidade pedida, recebida e saldo;
- conferência de notas com retorno estruturado de todas as divergências;
- histórico imutável das conferências;
- paginação estável por snapshot, evitando mudança de visão entre páginas;
- relatório paginado com filtros, aprovações, rejeições e motivos.

Documentação produzida até aqui:

- [Regras de negócio e premissas](docs/business-rules.md)
- [Desenho do banco de dados](docs/database-design.md)
- [Contrato OpenAPI](docs/openapi.yaml)
- [Arquitetura](docs/architecture.md)
- [Impacto da Parte 2](docs/part-2-impact.md)
- [Uso de IA](AI_USAGE.md)

## Endpoints disponíveis

```text
POST /api/v1/imports/alfa
POST /api/v1/imports/beta
POST /api/v1/imports/gama
POST /api/v1/imports/delta
GET  /api/v1/purchase-orders
GET  /api/v1/purchase-orders/{source}/{purchaseOrderNumber}
POST /api/v1/purchase-orders/{source}/{purchaseOrderNumber}/invoice-validations
GET  /api/v1/invoice-validations/{validationId}
GET  /api/v1/reports/invoice-validations
```

A listagem aceita `source`, `vendorTaxId`, `status`, `pendingOnly`, `snapshotId`, `page` e `size` como parâmetros opcionais. A primeira página materializa e retorna um `snapshotId`, que deve ser reutilizado com os mesmos filtros nas páginas seguintes. Durante 24 horas, o snapshot preserva a sequência e os dados resumidos dos pedidos mesmo que novas cargas criem pedidos ou alterem status, fornecedor e saldo de pedidos existentes.

A importação do Beta usa `multipart/form-data` com as partes `headersFile` e `itemsFile`. Arquivos prontos para teste estão em `samples/beta`.

A importação do Delta usa `multipart/form-data` com `ordersFile` e `itemsFile`. Itens sem pedido correspondente são informados como `ORPHAN_ITEM`, enquanto os pedidos válidos são importados. Pedidos sem itens são aceitos porque as fontes Delta são independentes.

O relatório aceita `source`, `status`, `from`, `to`, `snapshotAt`, `page` e `size`. Datas usam ISO 8601 em UTC, por exemplo `2026-08-01T00:00:00Z`.

## Tecnologias

- Java 21
- Spring Boot 3.5
- Spring Web, Validation, Data JPA e Actuator
- PostgreSQL e Flyway
- springdoc-openapi/Swagger UI
- JUnit 5
- Maven
- Docker Compose

Foi escolhida a linha Spring Boot 3.5 por sua maturidade e compatibilidade com Java 21. A adoção da geração 4 poderá ser avaliada posteriormente, sem trazer esse custo de migração para o escopo do case.

## Execução local com Docker

Pré-requisito: Docker Desktop em execução.

```bash
docker compose up --build
```

Após a inicialização:

- API: `http://localhost:8080`
- Health check: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

Para encerrar os containers sem apagar os dados:

```bash
docker compose down
```

Para também remover o volume do PostgreSQL e reiniciar com banco vazio:

```bash
docker compose down -v
```

## Execução sem Docker

Pré-requisitos: Java 21 e PostgreSQL disponível.

Variáveis aceitas:

```text
DB_URL=jdbc:postgresql://localhost:5432/v360
DB_USERNAME=v360
DB_PASSWORD=v360
```

Execute:

```bash
./mvnw spring-boot:run
```

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Testes

```bash
./mvnw verify
```

Os testes usam H2 em modo de compatibilidade PostgreSQL, executam as migrations Flyway e validam os fluxos HTTP com MockMvc.

## Logs

A aplicação registra cada requisição com `requestId`, método, rota, status HTTP e duração. Erros de negócio aparecem em nível `WARN`; falhas inesperadas aparecem em `ERROR` com stack trace. Corpos de requisição, CNPJ e credenciais não são escritos nos logs.

Os eventos são enviados ao console e ao arquivo `logs/v360-connector.log`. Com Docker, acompanhe pelo console:

```bash
docker compose logs -f api
```

Ou leia o arquivo persistido no diretório `logs` do projeto. O cabeçalho `X-Request-Id` da resposta permite localizar nos logs tudo que pertence a uma chamada específica. O nível pode ser alterado com `LOG_LEVEL`, por exemplo `LOG_LEVEL=DEBUG`.

## Como experimentar

1. Suba a aplicação.
2. Importe [postman/V360.postman_collection.json](postman/V360.postman_collection.json).
3. No Postman Desktop, abra `Settings > General > Working Directory` e selecione a pasta `postman` deste repositório.
4. Confirme que o Postman possui permissão para ler arquivos desse diretório.
5. Execute as pastas da coleção na ordem numérica.

Exemplo no Windows:

```text
C:\caminho\para\v360-purchase-order-connector\postman
```

A coleção referencia os arquivos como `../samples/...`. Por isso, o diretório de trabalho deve ser exatamente a pasta `postman`, e não a raiz do projeto nem a pasta padrão `C:\Users\<usuario>\Postman`. Um aviso amarelo no campo do arquivo ou um erro `ENOENT` indica que o Postman ainda está procurando os samples no diretório errado.

Como alternativa ao Postman, use [requests/v360.http](requests/v360.http) em uma IDE compatível com arquivos HTTP.

As cargas originais estão em `samples/alfa`, `samples/beta`, `samples/gama` e `samples/delta`.

## Decisões importantes

- O pedido é identificado externamente por `source + number`.
- CNPJ é comparado após remoção da máscara.
- Dinheiro e quantidades usam precisão decimal.
- A conferência agrega linhas repetidas antes de validar o saldo.
- A tolerância do total é de R$ 0,01 após arredondamento `HALF_UP`.
- A conferência não altera a quantidade recebida; a origem continua sendo a autoridade do saldo.
- Erros estruturais tornam a importação atômica; no Delta, itens órfãos são rejeitados individualmente e os pedidos válidos são preservados.

As justificativas completas estão em [docs/business-rules.md](docs/business-rules.md).

## O que eu faria com mais tempo

- testes de integração com PostgreSQL real usando Testcontainers;
- processamento assíncrono e parcial para cargas muito grandes;
- identificador único da nota para idempotência da conferência;
- autenticação e autorização;
- métricas de duração e falha por integração;
- política de tolerância configurável por cliente e moeda.

## Evolução após a Parte 1

Gama, Delta e as evoluções de consulta foram implementados após a tag `parte-1`. O impacto arquitetural, incluindo o que foi apenas adicionado e o que precisou ser modificado, está em [docs/part-2-impact.md](docs/part-2-impact.md).

### O que mudou para o Gama

- Foram adicionados contrato, agrupador e mapper próprios para o JSON achatado.
- Timestamp Unix, situação numérica, centavos, caixas e fatores de conversão são normalizados antes de entrar no domínio.
- O item passou a guardar unidade de compra, fator e preço originais para auditoria.
- A migration V3 adicionou esses campos opcionais sem invalidar dados Alfa e Beta.

### O que mudou para o Delta

- Foram adicionados dois contratos JSON e um assembler que relaciona itens e cabeçalhos pelo número do pedido.
- Itens órfãos são rejeitados explicitamente, enquanto pedidos válidos continuam sendo importados; pedidos sem itens são aceitos.
- O domínio passou a aceitar pedidos sem linhas e a preservar a data própria do item.
- A migration V4 adicionou a data da linha, habilitou `DELTA` e criou índices de consulta.

### O que mudou para paginação e relatório

- As migrations V5 e V6 adicionaram o instante estável e índices do relatório.
- A migration V7 criou snapshots materializados da listagem de pedidos. Isso garante que uma varredura não tenha duplicações nem omissões causadas por importações ou reimportações concorrentes.
- Consulta, conferência e relatório continuam usando o mesmo modelo normalizado, sem regras condicionais específicas de Gama ou Delta.
