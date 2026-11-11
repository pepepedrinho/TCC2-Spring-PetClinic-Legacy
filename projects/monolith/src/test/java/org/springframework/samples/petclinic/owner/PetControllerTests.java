package org.springframework.samples.petclinic.owner;

import java.sql.Date;
import java.util.Arrays;
import java.util.LinkedHashSet;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.integration.OwnersServiceHttpClient;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetController;
import org.springframework.samples.petclinic.owner.PetRepository;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.PetTypeFormatter;
import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Test class for the {@link PetController}
 *
 * @author Colin But
 */
@RunWith(SpringRunner.class)
@WebMvcTest(value = PetController.class, includeFilters = @ComponentScan.Filter(value = PetTypeFormatter.class, type = FilterType.ASSIGNABLE_TYPE))
public class PetControllerTests {

    private static final int TEST_OWNER_ID = 1;

    private static final int TEST_PET_ID = 1;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PetRepository petRepository;

    @MockBean
    private OwnersServiceHttpClient ownersServiceHttpClient;

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/test/java/org/springframework/samples/petclinic/owner/PetControllerTests.java: setup
    // ============================================================
    // @Before
    //     public void setup() {
    //         PetType cat = new PetType();
    //         cat.setId(3);
    //         cat.setName("hamster");
    //         given(this.pets.findPetTypes()).willReturn(Lists.newArrayList(cat));
    //         given(this.owners.findById(TEST_OWNER_ID)).willReturn(new Owner());
    //         given(this.pets.findById(TEST_PET_ID)).willReturn(new Pet());
    //
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O cenário da V2 utiliza Owner identificado e vínculo explícito com Pet.
    // ============================================================

    @Before
    public void setup() {
        PetType petType = new PetType();
        petType.setId(3);
        petType.setName("hamster");
        given(this.petRepository.findPetTypes()).willReturn(Lists.newArrayList(petType));
        Owner owner = new Owner();
        owner.setId(TEST_OWNER_ID);
        Pet pet = new Pet();
        pet.setId(TEST_PET_ID);
        pet.setName("Leo");
        owner.addPet(pet);
        given(this.ownersServiceHttpClient.findById(TEST_OWNER_ID)).willReturn(owner);
        given(this.petRepository.findById(TEST_PET_ID)).willReturn(pet);

    }

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
        verifyZeroInteractions(ownersServiceHttpClient);
    }

    @Test
    public void returnsEmptyObjectWithoutQueryingForEmptyBatch() throws Exception {
        mockMvc.perform(post("/internal/owner-pets").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}", true));

        verifyZeroInteractions(petRepository, ownersServiceHttpClient);
    }

    @Test
    public void rejectsInternalRequestsWithGatewayHeader() throws Exception {
        mockMvc.perform(post("/internal/owner-pets").contentType(MediaType.APPLICATION_JSON)
                .header("X-PetClinic-Gateway", "false").content("[1]"))
                .andExpect(status().isNotFound());

        verifyZeroInteractions(petRepository, ownersServiceHttpClient);
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

    @Test
    public void rejectsPetFromAnotherOwner() throws Exception {
        Owner other = new Owner();
        other.setId(2);
        Pet pet = new Pet();
        other.addPet(pet);
        given(this.petRepository.findById(TEST_PET_ID)).willReturn(pet);
        mockMvc.perform(get("/owners/1/pets/1/edit")).andExpect(status().isNotFound());
    }

    @Test
    public void testInitCreationForm() throws Exception {
        mockMvc.perform(get("/owners/{ownerId}/pets/new", TEST_OWNER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("pets/createOrUpdatePetForm"))
                .andExpect(model().attributeExists("pet"));
    }

    @Test
    public void testProcessCreationFormSuccess() throws Exception {
        mockMvc.perform(post("/owners/{ownerId}/pets/new", TEST_OWNER_ID)
                .param("name", "Betty")
                .param("type", "hamster")
                .param("birthDate", "2015-02-12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/owners/{ownerId}"));
    }

    @Test
    public void testProcessCreationFormHasErrors() throws Exception {
        mockMvc.perform(post("/owners/{ownerId}/pets/new", TEST_OWNER_ID)
                .param("name", "Betty")
                .param("birthDate", "2015-02-12"))
                .andExpect(model().attributeHasNoErrors("owner"))
                .andExpect(model().attributeHasErrors("pet"))
                .andExpect(model().attributeHasFieldErrors("pet", "type"))
                .andExpect(model().attributeHasFieldErrorCode("pet", "type", "required"))
                .andExpect(status().isOk())
                .andExpect(view().name("pets/createOrUpdatePetForm"));
    }

    @Test
    public void testInitUpdateForm() throws Exception {
        mockMvc.perform(get("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("pet"))
                .andExpect(view().name("pets/createOrUpdatePetForm"));
    }

    @Test
    public void testProcessUpdateFormSuccess() throws Exception {
        mockMvc.perform(post("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID)
                .param("name", "Betty")
                .param("type", "hamster")
                .param("birthDate", "2015-02-12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/owners/{ownerId}"));
    }

    @Test
    public void testProcessUpdateFormHasErrors() throws Exception {
        mockMvc.perform(post("/owners/{ownerId}/pets/{petId}/edit", TEST_OWNER_ID, TEST_PET_ID)
                .param("name", "Betty")
                .param("birthDate", "2015/02/12"))
                .andExpect(model().attributeHasNoErrors("owner"))
                .andExpect(model().attributeHasErrors("pet"))
                .andExpect(status().isOk())
                .andExpect(view().name("pets/createOrUpdatePetForm"));
    }

}
