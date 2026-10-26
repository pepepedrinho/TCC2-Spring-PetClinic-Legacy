package org.springframework.samples.petclinic.integration;

import org.junit.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.MediaType.APPLICATION_JSON;

public class OwnersPetsClientTest {

    @Test
    public void shouldFetchOwnerFromMicroservice() throws Exception {
        OwnersPetsClient client = new OwnersPetsClient("http://localhost:8082");

        Field field = OwnersPetsClient.class.getDeclaredField("restTemplate");
        field.setAccessible(true);

        RestTemplate restTemplate = (RestTemplate) field.get(client);
        MockRestServiceServer server =
                MockRestServiceServer.createServer(restTemplate);

        String json = "{\"id\":1,\"firstName\":\"George\","
                + "\"lastName\":\"Franklin\",\"address\":\"110 W. Liberty St.\","
                + "\"city\":\"Madison\",\"telephone\":\"6085551023\"}";

        server.expect(requestTo("http://localhost:8082/api/owners/1"))
                .andRespond(withSuccess(json, APPLICATION_JSON));

        OwnersPetsClient.OwnerResponse owner = client.findOwnerById(1);

        assertEquals(Integer.valueOf(1), owner.getId());
        assertEquals("George", owner.getFirstName());
        assertEquals("Franklin", owner.getLastName());
        assertEquals("Madison", owner.getCity());

        server.verify();
    }

    @Test
    public void shouldCreateOwner() throws Exception {
        OwnersPetsClient client = new OwnersPetsClient("http://localhost:8082");

        Field field = OwnersPetsClient.class.getDeclaredField("restTemplate");
        field.setAccessible(true);

        RestTemplate restTemplate = (RestTemplate) field.get(client);
        MockRestServiceServer server =
                MockRestServiceServer.createServer(restTemplate);

        org.springframework.samples.petclinic.owner.Owner owner =
                new org.springframework.samples.petclinic.owner.Owner();
        owner.setFirstName("Ana");
        owner.setLastName("Silva");
        owner.setAddress("Rua A");
        owner.setCity("Curitiba");
        owner.setTelephone("4199999999");

        server.expect(requestTo("http://localhost:8082/api/owners"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.method(
                        org.springframework.http.HttpMethod.POST))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(org.springframework.http.HttpStatus.CREATED)
                        .location(java.net.URI.create("/api/owners/11")));

        int id = client.createOwner(new OwnerRequest(owner));

        assertEquals(11, id);
        server.verify();
    }

    @Test
    public void shouldUpdateOwner() throws Exception {
        OwnersPetsClient client = new OwnersPetsClient("http://localhost:8082");

        Field field = OwnersPetsClient.class.getDeclaredField("restTemplate");
        field.setAccessible(true);

        RestTemplate restTemplate = (RestTemplate) field.get(client);
        MockRestServiceServer server =
                MockRestServiceServer.createServer(restTemplate);

        org.springframework.samples.petclinic.owner.Owner owner =
                new org.springframework.samples.petclinic.owner.Owner();
        owner.setFirstName("George");
        owner.setLastName("Franklin");
        owner.setAddress("Endereco atualizado");
        owner.setCity("Madison");
        owner.setTelephone("6085551023");

        server.expect(requestTo("http://localhost:8082/api/owners/1"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.method(
                        org.springframework.http.HttpMethod.PUT))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(org.springframework.http.HttpStatus.NO_CONTENT));

        client.updateOwner(1, new OwnerRequest(owner));
        server.verify();
    }}