package org.springframework.samples.petclinic.integration;

import org.junit.Test;
import static org.junit.Assert.*;

public class OwnersPetsLiveIT {

    @Test
    public void shouldFetchRealOwnerFromMicroservice() {
        OwnersPetsClient client =
                new OwnersPetsClient("http://localhost:8082");

        OwnersPetsClient.OwnerResponse owner = client.findOwnerById(1);

        assertNotNull(owner);
        assertEquals(Integer.valueOf(1), owner.getId());
        assertEquals("George", owner.getFirstName());
        assertEquals("Franklin", owner.getLastName());
    }
}