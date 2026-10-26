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
    }
    @Test
    public void shouldFindPetTypes() throws Exception {
        OwnersPetsClient client = new OwnersPetsClient("http://localhost:8082");

        Field field = OwnersPetsClient.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        RestTemplate restTemplate = (RestTemplate) field.get(client);

        MockRestServiceServer server =
                MockRestServiceServer.createServer(restTemplate);

        server.expect(requestTo("http://localhost:8082/api/pet-types"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withSuccess(
                                "[{\"id\":1,\"name\":\"cat\"},{\"id\":2,\"name\":\"dog\"}]",
                                org.springframework.http.MediaType.APPLICATION_JSON));

        OwnersPetsClient.PetTypeResponse[] types = client.findPetTypes();

        assertEquals(2, types.length);
        assertEquals(Integer.valueOf(1), types[0].getId());
        assertEquals("cat", types[0].getName());
        assertEquals(Integer.valueOf(2), types[1].getId());
        assertEquals("dog", types[1].getName());

        server.verify();
    }
    @Test
    public void shouldFindPetById() throws Exception {
        OwnersPetsClient client = new OwnersPetsClient("http://localhost:8082");

        Field field = OwnersPetsClient.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        RestTemplate restTemplate = (RestTemplate) field.get(client);

        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        server.expect(requestTo("http://localhost:8082/api/pets/7"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withSuccess(
                                "{\"id\":7,\"name\":\"Samantha\",\"birthDate\":\"2012-09-04\",\"typeId\":1,\"typeName\":\"cat\",\"ownerId\":6}",
                                org.springframework.http.MediaType.APPLICATION_JSON));

        OwnersPetsClient.PetResponse pet = client.findPetById(7);

        assertEquals(Integer.valueOf(7), pet.getId());
        assertEquals("Samantha", pet.getName());
        assertEquals("2012-09-04", pet.getBirthDate());
        assertEquals(Integer.valueOf(6), pet.getOwnerId());

        server.verify();
    }

    @Test
    public void shouldCreatePet() throws Exception {
        OwnersPetsClient client = new OwnersPetsClient("http://localhost:8082");

        Field field = OwnersPetsClient.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        RestTemplate restTemplate = (RestTemplate) field.get(client);

        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        org.springframework.samples.petclinic.owner.Pet pet =
                new org.springframework.samples.petclinic.owner.Pet();
        pet.setName("Rex");
        pet.setBirthDate(java.sql.Date.valueOf("2020-05-10"));

        org.springframework.samples.petclinic.owner.PetType type =
                new org.springframework.samples.petclinic.owner.PetType();
        type.setId(2);
        type.setName("dog");
        pet.setType(type);

        server.expect(requestTo("http://localhost:8082/api/owners/12/pets"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.method(
                        org.springframework.http.HttpMethod.POST))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(org.springframework.http.HttpStatus.CREATED)
                        .location(java.net.URI.create("/api/pets/14")));

        int id = client.createPet(12, new PetRequest(pet));

        assertEquals(14, id);
        server.verify();
    }

    @Test
    public void shouldUpdatePet() throws Exception {
        OwnersPetsClient client = new OwnersPetsClient("http://localhost:8082");

        Field field = OwnersPetsClient.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        RestTemplate restTemplate = (RestTemplate) field.get(client);

        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        org.springframework.samples.petclinic.owner.Pet pet =
                new org.springframework.samples.petclinic.owner.Pet();
        pet.setName("Rex");
        pet.setBirthDate(java.sql.Date.valueOf("2020-05-10"));

        org.springframework.samples.petclinic.owner.PetType type =
                new org.springframework.samples.petclinic.owner.PetType();
        type.setId(2);
        type.setName("dog");
        pet.setType(type);

        server.expect(requestTo("http://localhost:8082/api/pets/14"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.method(
                        org.springframework.http.HttpMethod.PUT))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(org.springframework.http.HttpStatus.NO_CONTENT));

        client.updatePet(14, new PetRequest(pet));

        server.verify();
    }}