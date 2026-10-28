package org.springframework.samples.petclinic.integration;
import org.junit.*;
import org.springframework.http.*;
import org.springframework.web.client.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.samples.petclinic.owner.Owner;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.junit.Assert.*;
public class OwnersClientTests {
    private OwnersClient client;
    private MockRestServiceServer server;
    @Before public void setup() {
        client=new OwnersClient("http://localhost:8082");
        server=MockRestServiceServer.createServer((RestTemplate) ReflectionTestUtils.getField(client,"http"));
    }
    @Test public void readsOwner() {
        server.expect(requestTo("http://localhost:8082/api/owners/1"))
            .andRespond(withSuccess("{\"id\":1,\"firstName\":\"George\"}",MediaType.APPLICATION_JSON));
        assertEquals("George",client.findById(1).getFirstName()); server.verify();
    }
    @Test public void prefixIsEncoded() {
        server.expect(requestTo("http://localhost:8082/api/owners?lastName=A%26B"))
            .andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        assertTrue(client.findByLastName("A&B").isEmpty());server.verify();
    }
    @Test(expected=OwnerNotFoundException.class) public void maps404() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.NOT_FOUND));client.findById(999);
    }
    @Test(expected=OwnerValidationException.class) public void maps400() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.BAD_REQUEST));client.save(new Owner());
    }
    @Test(expected=OwnersUnavailableException.class) public void mapsServerFailure() {
        server.expect(anything()).andRespond(withServerError());client.findById(1);
    }
    @Test(expected=OwnersUnavailableException.class) public void mapsTimeout() {
        server.expect(anything()).andRespond(request -> {throw new java.net.SocketTimeoutException("read timed out");});
        client.findById(1);
    }
    @Test(expected=OwnersUnavailableException.class) public void mapsMalformedBody() {
        server.expect(anything()).andRespond(withSuccess("not json",MediaType.APPLICATION_JSON));client.findById(1);
    }
    @Test public void createSendsOnlyOwnerFields() {
        Owner owner=new Owner();owner.setFirstName("Ana");
        server.expect(anything()).andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("pets"))))
            .andRespond(withSuccess("{\"id\":11}",MediaType.APPLICATION_JSON));
        assertEquals(Integer.valueOf(11),client.save(owner).getId());server.verify();
    }
}
