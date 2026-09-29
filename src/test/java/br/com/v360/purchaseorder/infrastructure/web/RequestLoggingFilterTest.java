package br.com.v360.purchaseorder.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    void addsAValidRequestIdToEveryResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String requestId = response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER);
        assertThat(requestId).isNotBlank();
        assertThatCode(() -> UUID.fromString(requestId)).doesNotThrowAnyException();
    }
}
