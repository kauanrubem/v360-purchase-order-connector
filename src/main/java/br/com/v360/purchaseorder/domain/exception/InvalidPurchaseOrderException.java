package br.com.v360.purchaseorder.domain.exception;

public class InvalidPurchaseOrderException extends RuntimeException {

    public InvalidPurchaseOrderException(String message) {
        super(message);
    }
}

