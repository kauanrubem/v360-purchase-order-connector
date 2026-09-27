# Execução da coleção Postman

Importe `V360.postman_collection.json` no Postman Desktop e configure o **Working Directory** como esta pasta `postman`.

Exemplo no Windows:

```text
C:\caminho\para\v360-purchase-order-connector\postman
```

Os caminhos da coleção são relativos a esta pasta:

| Importação | Arquivo referenciado |
|---|---|
| Alfa | `../samples/alfa/purchase-orders.json` |
| Beta | `../samples/beta/cabecalho.csv` e `../samples/beta/itens.csv` |
| Gama | `../samples/gama/purchase-orders.json` |
| Delta | `../samples/delta/orders.json` e `../samples/delta/items.json` |

Depois da configuração:

1. execute `01 - Health Check`;
2. execute as importações;
3. execute `Listar primeira página` antes de `Listar próxima página com mesmo snapshot`;
4. execute as conferências;
5. execute os relatórios.

Os scripts da coleção guardam automaticamente `validationId`, `purchaseOrderSnapshotId` e `reportSnapshotAt` quando as requisições anteriores são executadas com sucesso.

Se aparecer `ENOENT` ou um aviso amarelo no nome do arquivo, o Postman não está usando esta pasta como diretório de trabalho. Também é possível selecionar cada sample manualmente, mas isso não é necessário quando o Working Directory está correto.
