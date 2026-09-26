# Roteiro de demonstração

Duração sugerida: 8 a 10 minutos.

## 1. Contexto — 30 segundos

Explique que cada cliente fornece pedidos em um formato diferente e que a API transforma todos em um contrato único para consulta e conferência de notas.

## 2. Arquitetura — 60 segundos

Mostre `docs/architecture.md` e destaque:

- adaptadores isolados por cliente;
- modelo normalizado compartilhado;
- regras de conferência independentes da origem;
- PostgreSQL e Flyway;
- tag `parte-1` antes das integrações Gama e Delta.

## 3. Inicialização — 30 segundos

```bash
docker compose up --build
```

Mostre o health check e o Swagger:

```text
http://localhost:8080/actuator/health
http://localhost:8080/swagger-ui.html
```

## 4. Importações — 90 segundos

Na coleção Postman:

1. importe o Alfa;
2. importe os dois CSVs do Beta;
3. importe o JSON achatado do Gama;
4. importe os arquivos de pedidos e itens do Delta;
5. destaque que todos retornam o mesmo resumo de importação e que o item órfão do Delta é rejeitado sem impedir os pedidos válidos.

## 5. Normalização — 60 segundos

Consulte `GAMA/GL-778` e mostre:

- timestamp convertido em data;
- situação numérica convertida para `OPEN`;
- 10 caixas de 12 transformadas em 120 unidades;
- 2 caixas recebidas transformadas em 24 unidades;
- preço de R$ 1.200,00 por caixa transformado em R$ 100,00 por unidade;
- dados originais preservados em `sourceDetails`.

Depois consulte `DELTA/DL-2026-0044` e destaque que o conector correlacionou o pedido e seus itens, recebidos de fontes separadas, pelo número do pedido. Mostre também que `DL-2026-0046`, sem itens, continua disponível para consulta.

## 6. Conferência — 90 segundos

Execute uma nota aprovada e outra com fornecedor, quantidade e preço divergentes. Destaque que:

- todas as divergências são retornadas juntas;
- os valores esperado e recebido são estruturados;
- a conferência não altera o saldo;
- o histórico recebe um UUID consultável.

## 7. Paginação, relatório e evolução — 90 segundos

Liste pedidos com `size=2`, copie o `snapshotAt` retornado e use-o na página seguinte. Explique que isso mantém uma visão estável enquanto novas importações acontecem.

Mostre o relatório paginado com aprovadas, rejeitadas, motivos e filtro por status. Depois execute:

```bash
git log --oneline --decorate
git diff --stat parte-1..HEAD
```

Explique que consulta, conferência e relatório permanecem compartilhados entre Alfa, Beta, Gama e Delta; somente os adaptadores conhecem os formatos externos. Finalize mostrando `docs/part-2-impact.md` e `AI_USAGE.md`.

## Antes de gravar

- limpe o banco com `docker compose down -v`;
- feche notificações e terminais irrelevantes;
- confirme que o Postman pode acessar os arquivos locais;
- execute toda a coleção uma vez sem gravar;
- grave em 1080p e mantenha o texto legível.
