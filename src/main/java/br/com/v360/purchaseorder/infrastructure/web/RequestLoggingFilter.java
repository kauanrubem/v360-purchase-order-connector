package br.com.v360.purchaseorder.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();

        MDC.put("requestId", requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            if (response.getStatus() >= 400) {
                LOGGER.warn(
                        "HTTP request completed method={} path={} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs
                );
            } else {
                LOGGER.info(
                        "HTTP request completed method={} path={} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs
                );
            }
            MDC.remove("requestId");
        }
    }
}
