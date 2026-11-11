package org.springframework.samples.petclinic.integration;

import java.sql.Date;
import java.util.Arrays;
import java.util.LinkedHashSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.pets.Pet;
import org.springframework.samples.petclinic.pets.PetRepository;
import org.springframework.samples.petclinic.pets.PetType;
import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(PetRestController.class)
public class PetRestControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PetRepository petRepository;

    @Test
    public void returnsPetsAndVisitsForDistinctOwnersInOneBatch() throws Exception {
        Pet leo = summaryPet(1, "Leo", 1);
        Visit visit = new Visit();
        visit.setDate(Date.valueOf("2020-03-14"));
        visit.setDescription("checkup");
        leo.addVisit(visit);

        LinkedHashSet<Integer> owners = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        given(petRepository.findWithVisitsByOwnerIds(owners)).willReturn(Arrays.asList(
                summaryPet(4, "leo", 1), summaryPet(3, "Bella", 2), leo, summaryPet(2, "ava", 1)));

        mockMvc.perform(post("/internal/owner-pets").contentType(MediaType.APPLICATION_JSON)
                .content("[1,2,1,3]"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{"
                        + "\"1\":[{\"id\":2,\"name\":\"ava\",\"birthDate\":\"2010-09-07\",\"type\":\"cat\",\"visits\":[]},"
                        + "{\"id\":1,\"name\":\"Leo\",\"birthDate\":\"2010-09-07\",\"type\":\"cat\","
                        + "\"visits\":[{\"date\":\"2020-03-14\",\"description\":\"checkup\"}]},"
                        + "{\"id\":4,\"name\":\"leo\",\"birthDate\":\"2010-09-07\",\"type\":\"cat\",\"visits\":[]}],"
                        + "\"2\":[{\"id\":3,\"name\":\"Bella\",\"birthDate\":\"2010-09-07\",\"type\":\"cat\",\"visits\":[]}],"
                        + "\"3\":[]}", true));

        verify(petRepository).findWithVisitsByOwnerIds(owners);
        verifyNoMoreInteractions(petRepository);
    }

    @Test
    public void returnsEmptyObjectWithoutQueryingForEmptyBatch() throws Exception {
        mockMvc.perform(post("/internal/owner-pets").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}", true));

        verifyZeroInteractions(petRepository);
    }

    @Test
    public void rejectsInternalRequestsWithGatewayHeader() throws Exception {
        mockMvc.perform(post("/internal/owner-pets").contentType(MediaType.APPLICATION_JSON)
                .header("X-PetClinic-Gateway", "false").content("[1]"))
                .andExpect(status().isNotFound());

        verifyZeroInteractions(petRepository);
    }

    private Pet summaryPet(int id, String name, int ownerId) {
        Owner owner = new Owner();
        owner.setId(ownerId);
        PetType type = new PetType();
        type.setName("cat");
        Pet pet = new Pet();
        pet.setId(id);
        pet.setName(name);
        pet.setBirthDate(Date.valueOf("2010-09-07"));
        pet.setType(type);
        owner.addPet(pet);
        return pet;
    }
}
