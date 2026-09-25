package br.com.v360.purchaseorder.integration.delta;

import br.com.v360.purchaseorder.domain.exception.InvalidPurchaseOrderException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DeltaJsonParser {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public DeltaJsonParser(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public DeltaOrderPayload parseOrders(MultipartFile file) {
        return parseAndValidate(file, DeltaOrderPayload.class, "pedidos");
    }

    public DeltaItemPayload parseItems(MultipartFile file) {
        return parseAndValidate(file, DeltaItemPayload.class, "itens");
    }

    private <T> T parseAndValidate(MultipartFile file, Class<T> type, String description) {
        if (file == null || file.isEmpty()) {
            throw new InvalidPurchaseOrderException(
                    "O arquivo JSON de %s do Delta é obrigatório e não pode estar vazio.".formatted(description)
            );
        }
        try {
            T payload = objectMapper.readValue(file.getInputStream(), type);
            Set<ConstraintViolation<T>> violations = validator.validate(payload);
            if (!violations.isEmpty()) {
                String details = violations.stream()
                        .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                        .map(violation -> "%s: %s".formatted(
                                violation.getPropertyPath(),
                                violation.getMessage()
                        ))
                        .collect(Collectors.joining("; "));
                throw new InvalidPurchaseOrderException(
                        "O arquivo JSON de %s do Delta possui campos inválidos: %s"
                                .formatted(description, details)
                );
            }
            return payload;
        } catch (JsonProcessingException exception) {
            throw new InvalidPurchaseOrderException(
                    "O arquivo JSON de %s do Delta é inválido: %s"
                            .formatted(description, exception.getOriginalMessage())
            );
        } catch (IOException exception) {
            throw new InvalidPurchaseOrderException(
                    "Não foi possível ler o arquivo JSON de %s do Delta: %s"
                            .formatted(description, exception.getMessage())
            );
        }
    }
}
