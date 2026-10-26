package org.springframework.samples.petclinic.owner;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.samples.petclinic.integration.OwnersPetsClient;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.PetTypeFormatter;

/**
 * Test class for {@link PetTypeFormatter}
 *
 * @author Colin But
 */
@RunWith(MockitoJUnitRunner.class)
public class PetTypeFormatterTests {

    @Mock
    private OwnersPetsClient client;

    private PetTypeFormatter petTypeFormatter;

    @Before
    public void setup() {
        this.petTypeFormatter = new PetTypeFormatter(client);
    }

    @Test
    public void testPrint() {
        PetType petType = new PetType();
        petType.setName("Hamster");
        String petTypeName = this.petTypeFormatter.print(petType, Locale.ENGLISH);
        assertEquals("Hamster", petTypeName);
    }

    @Test
    public void shouldParse() throws ParseException {
        Mockito.when(this.client.findPetTypes()).thenReturn(makePetTypeResponses());
        PetType petType = petTypeFormatter.parse("Bird", Locale.ENGLISH);
        assertEquals("Bird", petType.getName());
    }

    @Test(expected = ParseException.class)
    public void shouldThrowParseException() throws ParseException {
        Mockito.when(this.client.findPetTypes()).thenReturn(makePetTypeResponses());
        petTypeFormatter.parse("Fish", Locale.ENGLISH);
    }

    /**
     * Helper method to produce some sample pet types just for test purpose
     *
     * @return {@link Collection} of {@link PetType}
     */
    private OwnersPetsClient.PetTypeResponse[] makePetTypeResponses() {
        OwnersPetsClient.PetTypeResponse dog = new OwnersPetsClient.PetTypeResponse();
        dog.setId(1);
        dog.setName("Dog");

        OwnersPetsClient.PetTypeResponse bird = new OwnersPetsClient.PetTypeResponse();
        bird.setId(2);
        bird.setName("Bird");

        return new OwnersPetsClient.PetTypeResponse[] { dog, bird };
    }
}
