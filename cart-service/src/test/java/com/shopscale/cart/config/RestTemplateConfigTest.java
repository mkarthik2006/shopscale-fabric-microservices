package com.shopscale.cart.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestTemplateConfigTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void restTemplate_shouldForwardAuthorizationHeaderFromIncomingRequest() {
        RestTemplateConfig config = new RestTemplateConfig();
        RestTemplate template = config.restTemplate(new RestTemplateBuilder());

        MockHttpServletRequest incoming = new MockHttpServletRequest();
        incoming.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(incoming));

        MockRestServiceServer server = MockRestServiceServer.bindTo(template).build();
        server.expect(once(), requestTo("http://example.com/resource"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        template.getForEntity("http://example.com/resource", String.class);
        server.verify();
    }
}
