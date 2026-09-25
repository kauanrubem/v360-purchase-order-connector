package br.com.v360.purchaseorder.integration.delta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeltaPayloadTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void deserializesOrderFieldsUsingDeltaContract() throws Exception {
        String json = """
                {
                  "orders": [{
                    "po_number": "DL-2026-0044",
                    "created_at": "2026-09-02",
                    "status": "open",
                    "currency": "BRL",
                    "vendor": {
                      "tax_id": "67890123000145",
                      "name": "Embalagens Norte Sul Ltda"
                    }
                  }]
                }
                """;

        DeltaOrderPayload payload = objectMapper.readValue(json, DeltaOrderPayload.class);

        assertThat(validator.validate(payload)).isEmpty();
        assertThat(payload.orders()).singleElement().satisfies(order -> {
            assertThat(order.number()).isEqualTo("DL-2026-0044");
            assertThat(order.createdAt()).isEqualTo(LocalDate.of(2026, 9, 2));
            assertThat(order.vendor().taxId()).isEqualTo("67890123000145");
        });
    }

    @Test
    void deserializesItemLinkAndItsOwnCreationDate() throws Exception {
        String json = """
                {
                  "items": [{
                    "purchase_order": "DL-2026-0044",
                    "created_at": "2026-09-08",
                    "line": 20,
                    "material": "EMB-720",
                    "description": "Fita adesiva 48mm",
                    "uom": "UN",
                    "quantity_ordered": 240,
                    "quantity_received": 0,
                    "unit_price": 8.2
                  }]
                }
                """;

        DeltaItemPayload payload = objectMapper.readValue(json, DeltaItemPayload.class);

        assertThat(validator.validate(payload)).isEmpty();
        assertThat(payload.items()).singleElement().satisfies(item -> {
            assertThat(item.purchaseOrderNumber()).isEqualTo("DL-2026-0044");
            assertThat(item.createdAt()).isEqualTo(LocalDate.of(2026, 9, 8));
            assertThat(item.unitPrice()).isEqualByComparingTo(new BigDecimal("8.2"));
        });
    }

    @Test
    void allowsEmptyIndependentSnapshotsButRejectsMissingCollections() {
        assertThat(validator.validate(new DeltaOrderPayload(List.of()))).isEmpty();
        assertThat(validator.validate(new DeltaItemPayload(List.of()))).isEmpty();
        assertThat(validator.validate(new DeltaOrderPayload(null)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("orders");
        assertThat(validator.validate(new DeltaItemPayload(null)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("items");
    }

    @Test
    void rejectsInvalidNestedFields() {
        DeltaItemPayload.DeltaItem invalidItem = new DeltaItemPayload.DeltaItem(
                " ",
                null,
                0,
                "",
                "",
                "",
                new BigDecimal("-1"),
                new BigDecimal("-1"),
                new BigDecimal("-1")
        );

        assertThat(validator.validate(new DeltaItemPayload(List.of(invalidItem))))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(
                        "items[0].purchaseOrderNumber",
                        "items[0].createdAt",
                        "items[0].line",
                        "items[0].materialCode",
                        "items[0].description",
                        "items[0].unitOfMeasure",
                        "items[0].quantityOrdered",
                        "items[0].quantityReceived",
                        "items[0].unitPrice"
                );
    }
}
