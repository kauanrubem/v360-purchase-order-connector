package br.com.v360.purchaseorder.integration.beta;

import br.com.v360.purchaseorder.domain.exception.InvalidPurchaseOrderException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

@Component
public class BetaCsvParser {

    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter
            .ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .get();

    public List<BetaHeaderRow> parseHeaders(MultipartFile file) {
        List<BetaHeaderRow> rows = new ArrayList<>();
        try (CSVParser parser = parser(file)) {
            validateHeaders(parser, List.of(
                    "NUMERO_PEDIDO",
                    "FORNECEDOR_CNPJ",
                    "FORNECEDOR_RAZAO_SOCIAL",
                    "EMISSAO",
                    "SITUACAO",
                    "MOEDA"
            ));
            for (CSVRecord record : parser) {
                rows.add(new BetaHeaderRow(
                        required(record, "NUMERO_PEDIDO"),
                        required(record, "FORNECEDOR_CNPJ"),
                        required(record, "FORNECEDOR_RAZAO_SOCIAL"),
                        parseDate(required(record, "EMISSAO"), record.getRecordNumber()),
                        required(record, "SITUACAO"),
                        required(record, "MOEDA")
                ));
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw invalidFile(file, exception);
        }
        if (rows.isEmpty()) {
            throw new InvalidPurchaseOrderException("O CSV de cabeçalhos do Beta está vazio.");
        }
        return rows;
    }

    public List<BetaItemRow> parseItems(MultipartFile file) {
        List<BetaItemRow> rows = new ArrayList<>();
        try (CSVParser parser = parser(file)) {
            validateHeaders(parser, List.of(
                    "NUMERO_PEDIDO",
                    "ITEM",
                    "CODIGO_MATERIAL",
                    "DESCRICAO",
                    "UNIDADE",
                    "QTD_PEDIDA",
                    "QTD_RECEBIDA",
                    "PRECO_UNITARIO"
            ));
            for (CSVRecord record : parser) {
                rows.add(new BetaItemRow(
                        required(record, "NUMERO_PEDIDO"),
                        required(record, "ITEM"),
                        required(record, "CODIGO_MATERIAL"),
                        required(record, "DESCRICAO"),
                        required(record, "UNIDADE"),
                        parseDecimal(required(record, "QTD_PEDIDA"), "QTD_PEDIDA", record.getRecordNumber()),
                        parseDecimal(required(record, "QTD_RECEBIDA"), "QTD_RECEBIDA", record.getRecordNumber()),
                        parseDecimal(required(record, "PRECO_UNITARIO"), "PRECO_UNITARIO", record.getRecordNumber())
                ));
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw invalidFile(file, exception);
        }
        if (rows.isEmpty()) {
            throw new InvalidPurchaseOrderException("O CSV de itens do Beta está vazio.");
        }
        return rows;
    }

    private CSVParser parser(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new InvalidPurchaseOrderException("O arquivo CSV do Beta é obrigatório e não pode estar vazio.");
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        return new CSVParser(reader, FORMAT);
    }

    private void validateHeaders(CSVParser parser, List<String> expected) {
        List<String> missing = expected.stream()
                .filter(header -> !parser.getHeaderMap().containsKey(header))
                .toList();
        if (!missing.isEmpty()) {
            throw new InvalidPurchaseOrderException("Colunas obrigatórias ausentes no CSV Beta: " + missing);
        }
    }

    private String required(CSVRecord record, String column) {
        String value = record.get(column).trim();
        if (value.isEmpty()) {
            throw new InvalidPurchaseOrderException(
                    "Campo %s vazio na linha %d do CSV Beta.".formatted(column, record.getRecordNumber())
            );
        }
        return value;
    }

    private LocalDate parseDate(String value, long row) {
        try {
            return LocalDate.parse(value, BRAZILIAN_DATE);
        } catch (DateTimeParseException exception) {
            throw new InvalidPurchaseOrderException(
                    "Data inválida na linha %d do CSV Beta: %s".formatted(row, value)
            );
        }
    }

    private BigDecimal parseDecimal(String value, String column, long row) {
        try {
            String normalized = value.replace(".", "").replace(',', '.');
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            throw new InvalidPurchaseOrderException(
                    "Número inválido na coluna %s, linha %d do CSV Beta: %s".formatted(column, row, value)
            );
        }
    }

    private InvalidPurchaseOrderException invalidFile(MultipartFile file, Exception exception) {
        if (exception instanceof InvalidPurchaseOrderException invalid) {
            return invalid;
        }
        return new InvalidPurchaseOrderException(
                "Não foi possível ler o arquivo CSV Beta %s: %s".formatted(file.getOriginalFilename(), exception.getMessage())
        );
    }

    public record BetaHeaderRow(
            String number,
            String vendorTaxId,
            String vendorName,
            LocalDate createdAt,
            String status,
            String currency
    ) {
    }

    public record BetaItemRow(
            String orderNumber,
            String line,
            String materialCode,
            String description,
            String unitOfMeasure,
            BigDecimal quantityOrdered,
            BigDecimal quantityReceived,
            BigDecimal unitPrice
    ) {
    }
}

