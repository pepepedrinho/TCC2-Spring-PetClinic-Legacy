package org.springframework.samples.petclinic.integration;

import org.junit.*;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.*;

import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

// Teste novo da integração: verifica a consulta de Owner e a tradução de respostas e falhas HTTP sem acessar um
// serviço real.
public class OwnersServiceHttpClientTests {
    private OwnersServiceHttpClient ownersServiceHttpClient;

    private MockRestServiceServer mockRestServiceServer;

    @Before
    public void setup() {
        ownersServiceHttpClient = new OwnersServiceHttpClient("http://localhost:8082");
        mockRestServiceServer = MockRestServiceServer
                .createServer((RestTemplate) ReflectionTestUtils.getField(ownersServiceHttpClient, "restTemplate"));
    }

    @Test
    public void readsOwner() {
        mockRestServiceServer.expect(requestTo("http://localhost:8082/api/owners/1"))
                .andRespond(withSuccess("{\"id\":1,\"firstName\":\"George\"}", MediaType.APPLICATION_JSON));
        assertEquals("George", ownersServiceHttpClient.findById(1).getFirstName());
        mockRestServiceServer.verify();
    }

    @Test
    public void maps404() {
        mockRestServiceServer.expect(anything()).andRespond(withStatus(HttpStatus.NOT_FOUND));
        expectFailure(HttpStatus.NOT_FOUND, () -> ownersServiceHttpClient.findById(999));
    }

    @Test
    public void maps400() {
        mockRestServiceServer.expect(anything()).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        expectFailure(HttpStatus.BAD_REQUEST, () -> ownersServiceHttpClient.findById(1));
    }

    @Test
    public void mapsServerFailure() {
        mockRestServiceServer.expect(anything()).andRespond(withServerError());
        expectFailure(HttpStatus.SERVICE_UNAVAILABLE, () -> ownersServiceHttpClient.findById(1));
    }

    @Test
    public void mapsTimeout() {
        mockRestServiceServer.expect(anything()).andRespond(request -> {
            throw new java.net.SocketTimeoutException("read timed out");
        });
        expectFailure(HttpStatus.SERVICE_UNAVAILABLE, () -> ownersServiceHttpClient.findById(1));
    }

    @Test
    public void mapsMalformedBody() {
        mockRestServiceServer.expect(anything()).andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));
        expectFailure(HttpStatus.SERVICE_UNAVAILABLE, () -> ownersServiceHttpClient.findById(1));
    }

    @Test
    public void mapsConnectionFailure() {
        mockRestServiceServer.expect(anything()).andRespond(request -> {
            throw new java.net.ConnectException("connection refused");
        });
        expectFailure(HttpStatus.SERVICE_UNAVAILABLE, () -> ownersServiceHttpClient.findById(1));
    }

    @Test
    public void doesNotMaskProgrammingError() {
        IllegalStateException original = new IllegalStateException("internal failure");
        mockRestServiceServer.expect(anything()).andRespond(request -> {
            throw original;
        });
        try {
            ownersServiceHttpClient.findById(1);
            fail("Programming error must escape the integration boundary");
        } catch (IllegalStateException actual) {
            assertSame(original, actual);
        }
        mockRestServiceServer.verify();
    }

    // Antes os testes esperavam três tipos; agora verificam o status e a causa do único marcador.
    private void expectFailure(HttpStatus status, Runnable call) {
        try {
            call.run();
            fail("Expected integration failure");
        } catch (IntegrationErrorHandler.IntegrationFailure failure) {
            assertEquals(status, failure.getStatus());
            assertNotNull(failure.getCause());
        }
        mockRestServiceServer.verify();
    }
}
