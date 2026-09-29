package br.com.v360.purchaseorder.infrastructure.web;

import br.com.v360.purchaseorder.domain.exception.InvalidPurchaseOrderException;
import br.com.v360.purchaseorder.domain.exception.InvalidRequestException;
import br.com.v360.purchaseorder.domain.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        logExpected(HttpStatus.NOT_FOUND, exception, request);
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidPurchaseOrderException.class)
    ProblemDetail handleInvalidPurchaseOrder(InvalidPurchaseOrderException exception, HttpServletRequest request) {
        logExpected(HttpStatus.BAD_REQUEST, exception, request);
        return problem(HttpStatus.BAD_REQUEST, "Pedido inválido", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail handleInvalidRequest(InvalidRequestException exception, HttpServletRequest request) {
        logExpected(HttpStatus.BAD_REQUEST, exception, request);
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleBeanValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        logExpected(HttpStatus.BAD_REQUEST, exception, request);
        ProblemDetail detail = problem(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida",
                "Existem campos inválidos na requisição.",
                request
        );
        List<FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        logExpected(HttpStatus.BAD_REQUEST, exception, request);
        return problem(HttpStatus.BAD_REQUEST, "Parâmetro inválido", exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error(
                "Unexpected error method={} path={}",
                request.getMethod(), request.getRequestURI(), exception
        );
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno",
                "Ocorreu um erro inesperado. Use o X-Request-Id da resposta para consultar os logs.",
                request
        );
    }

    private void logExpected(HttpStatus status, Exception exception, HttpServletRequest request) {
        LOGGER.warn(
                "Request rejected method={} path={} status={} exception={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                status.value(),
                exception.getClass().getSimpleName(),
                exception.getMessage()
        );
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String message,
            HttpServletRequest request
    ) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(title);
        detail.setType(URI.create("https://api.v360.local/problems/" + status.value()));
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }

    private record FieldError(String field, String message) {
    }
}
