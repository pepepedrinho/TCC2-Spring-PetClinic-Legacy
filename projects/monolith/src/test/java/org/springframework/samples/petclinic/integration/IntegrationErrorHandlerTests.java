package org.springframework.samples.petclinic.integration;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.stereotype.Controller;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.NestedServletException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// Exercita a fronteira HTTP real do cliente e o advice MVC, incluindo falhas que não pertencem a ele.
public class IntegrationErrorHandlerTests {
    private MockMvc mockMvc;

    private MockRestServiceServer mockRestServiceServer;

    @Before
    public void setup() {
        OwnersServiceHttpClient ownersServiceHttpClient = new OwnersServiceHttpClient("http://localhost:8082");
        mockRestServiceServer = MockRestServiceServer
                .createServer((RestTemplate) ReflectionTestUtils.getField(ownersServiceHttpClient, "restTemplate"));
        mockMvc = MockMvcBuilders.standaloneSetup(new IntegrationEndpoints(ownersServiceHttpClient))
                .setControllerAdvice(new IntegrationErrorHandler())
                .build();
    }

    @Test
    public void missingOwnerUses404AndOriginalReason() throws Exception {
        mockRestServiceServer.expect(anything()).andRespond(withStatus(HttpStatus.NOT_FOUND));
        mockMvc.perform(get("/integration-test"))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Owner or Pet not found"));
        mockRestServiceServer.verify();
    }

    @Test
    public void rejectedDataUses400AndOriginalReason() throws Exception {
        mockRestServiceServer.expect(anything()).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        mockMvc.perform(get("/integration-test"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("Invalid owner data"));
        mockRestServiceServer.verify();
    }

    @Test
    public void unavailableUses503AndExistingPage() throws Exception {
        mockRestServiceServer.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        expectUnavailable("/integration-test");
        mockRestServiceServer.verify();
    }

    @Test
    public void otherRemoteStatusKeepsUnavailableBehavior() throws Exception {
        mockRestServiceServer.expect(anything()).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        expectUnavailable("/integration-test");
        mockRestServiceServer.verify();
    }

    @Test
    public void timeoutUses503AndExistingPage() throws Exception {
        mockRestServiceServer.expect(anything()).andRespond(request -> {
            throw new java.net.SocketTimeoutException("read timed out");
        });
        expectUnavailable("/integration-test");
        mockRestServiceServer.verify();
    }

    @Test
    public void connectionFailureUses503AndExistingPage() throws Exception {
        mockRestServiceServer.expect(anything()).andRespond(request -> {
            throw new java.net.ConnectException("connection refused");
        });
        expectUnavailable("/integration-test");
        mockRestServiceServer.verify();
    }

    @Test
    public void malformedRemoteResponseUses503AndExistingPage() throws Exception {
        mockRestServiceServer.expect(anything()).andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));
        expectUnavailable("/integration-test");
        mockRestServiceServer.verify();
    }

    @Test
    public void explicitConversionFailureUses503AndExistingPage() throws Exception {
        expectUnavailable("/conversion-test");
    }

    @Test
    public void invalidLocalOwnershipUses404() throws Exception {
        mockMvc.perform(get("/ownership-test"))
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Owner or Pet not found"));
    }

    @Test
    public void programmingErrorIsNotHandledByIntegrationAdvice() throws Exception {
        expectUnhandled("/internal-test", IllegalStateException.class);
    }

    @Test
    public void unrelatedHttpErrorIsNotHandledByIntegrationAdvice() throws Exception {
        expectUnhandled("/unrelated-http-test", ResourceAccessException.class);
    }

    private void expectUnavailable(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("ownersUnavailable"));
    }

    private void expectUnhandled(String path, Class<? extends RuntimeException> expected) throws Exception {
        try {
            mockMvc.perform(get(path));
            fail("Unrelated failure must escape this advice");
        } catch (NestedServletException ex) {
            assertTrue(expected.isInstance(ex.getCause()));
        }
    }

    // Fixture exclusiva dos testes; não adiciona endpoints à aplicação.
    @Controller
    public static class IntegrationEndpoints {
        private final OwnersServiceHttpClient ownersServiceHttpClient;

        public IntegrationEndpoints(OwnersServiceHttpClient ownersServiceHttpClient) {
            this.ownersServiceHttpClient = ownersServiceHttpClient;
        }

        @GetMapping("/integration-test")
        @ResponseBody
        public Owner read() {
            return ownersServiceHttpClient.findById(1);
        }

        @GetMapping("/conversion-test")
        @ResponseBody
        public Owner conversion() {
            throw IntegrationErrorHandler.remoteFailure(new HttpMessageConversionException("invalid response"));
        }

        @GetMapping("/ownership-test")
        @ResponseBody
        public Owner ownership() {
            throw IntegrationErrorHandler.notFound();
        }

        @GetMapping("/internal-test")
        @ResponseBody
        public Owner internal() {
            throw new IllegalStateException("internal programming error");
        }

        @GetMapping("/unrelated-http-test")
        @ResponseBody
        public Owner unrelatedHttp() {
            throw new ResourceAccessException("unrelated outbound request");
        }
    }
}
