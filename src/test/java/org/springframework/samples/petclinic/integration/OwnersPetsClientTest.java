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
}