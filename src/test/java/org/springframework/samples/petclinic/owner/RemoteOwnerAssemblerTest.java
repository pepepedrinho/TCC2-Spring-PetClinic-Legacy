package org.springframework.samples.petclinic.owner;

import java.util.Arrays;
import java.util.Date;

import org.junit.Test;
import org.springframework.samples.petclinic.integration.OwnersPetsClient;
import org.springframework.samples.petclinic.integration.OwnersPetsClient.OwnerResponse;
import org.springframework.samples.petclinic.integration.OwnersPetsClient.PetResponse;
import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.samples.petclinic.visit.VisitRepository;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RemoteOwnerAssemblerTest {

    @Test
    public void shouldAssembleOwnerPetsAndVisits() {
        OwnersPetsClient client = mock(OwnersPetsClient.class);
        VisitRepository visits = mock(VisitRepository.class);

        OwnerResponse ownerResponse = new OwnerResponse();
        ownerResponse.setId(1);
        ownerResponse.setFirstName("George");
        ownerResponse.setLastName("Franklin");
        ownerResponse.setAddress("110 W. Liberty St.");
        ownerResponse.setCity("Madison");
        ownerResponse.setTelephone("6085551023");

        PetResponse petResponse = new PetResponse();
        petResponse.setId(1);
        petResponse.setName("Leo");
        petResponse.setBirthDate("2010-09-07");
        petResponse.setTypeId(1);
        petResponse.setTypeName("cat");
        petResponse.setOwnerId(1);

        ownerResponse.setPets(Arrays.asList(petResponse));

        Visit visit = new Visit();
        visit.setId(1);
        visit.setPetId(1);
        visit.setDate(new Date());
        visit.setDescription("Regular checkup");

        when(client.findOwnerById(1)).thenReturn(ownerResponse);
        when(visits.findByPetId(1)).thenReturn(Arrays.asList(visit));

        RemoteOwnerAssembler assembler =
                new RemoteOwnerAssembler(client, visits);

        Owner owner = assembler.findById(1);

        assertNotNull(owner);
        assertEquals(Integer.valueOf(1), owner.getId());
        assertEquals("George", owner.getFirstName());
        assertEquals(1, owner.getPets().size());

        Pet pet = owner.getPets().get(0);

        assertEquals(Integer.valueOf(1), pet.getId());
        assertEquals("Leo", pet.getName());
        assertEquals("cat", pet.getType().getName());
        assertEquals(owner, pet.getOwner());

        assertEquals(1, pet.getVisits().size());
        assertEquals("Regular checkup",
                pet.getVisits().get(0).getDescription());

        verify(client).findOwnerById(1);
        verify(visits).findByPetId(1);
    }

    @Test
    public void shouldFindOwnersByLastName() {
        OwnersPetsClient client = mock(OwnersPetsClient.class);
        VisitRepository visits = mock(VisitRepository.class);

        OwnerResponse betty = new OwnerResponse();
        betty.setId(2);
        betty.setFirstName("Betty");
        betty.setLastName("Davis");

        OwnerResponse harold = new OwnerResponse();
        harold.setId(3);
        harold.setFirstName("Harold");
        harold.setLastName("Davis");

        when(client.findOwnersByLastName("Dav"))
                .thenReturn(new OwnerResponse[] {betty, harold});

        RemoteOwnerAssembler assembler =
                new RemoteOwnerAssembler(client, visits);

        java.util.List<Owner> results = assembler.findByLastName("Dav");

        assertEquals(2, results.size());
        assertEquals("Betty", results.get(0).getFirstName());
        assertEquals("Harold", results.get(1).getFirstName());

        verify(client).findOwnersByLastName("Dav");
        verifyZeroInteractions(visits);
    }}