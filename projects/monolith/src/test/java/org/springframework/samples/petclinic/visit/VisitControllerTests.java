package org.springframework.samples.petclinic.visit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.integration.OwnersServiceHttpClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.pets.Pet;
import org.springframework.samples.petclinic.pets.PetRepository;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Test class for {@link VisitController}
 *
 * @author Colin But
 */
@RunWith(SpringRunner.class)
@WebMvcTest(VisitController.class)
public class VisitControllerTests {

    private static final int TEST_PET_ID = 1;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VisitRepository visitRepository;

    @MockBean
    private PetRepository petRepository;

    @MockBean
    private OwnersServiceHttpClient ownersServiceHttpClient;

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java: init
    // ============================================================
    // @Before
    //     public void init() {
    //         given(this.pets.findById(TEST_PET_ID)).willReturn(new Pet());
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O cenário da V2 representa o vínculo de Owner remoto com Pet local e a rota concreta.
    // ============================================================

    @Before
    public void init() {
        Owner owner = new Owner();
        owner.setId(1);
        Pet pet = new Pet();
        pet.setId(TEST_PET_ID);
        owner.addPet(pet);
        given(this.ownersServiceHttpClient.findById(1)).willReturn(owner);
        given(this.petRepository.findById(TEST_PET_ID)).willReturn(pet);
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java: testInitNewVisitForm
    // ============================================================
    // @Test
    //     public void testInitNewVisitForm() throws Exception {
    //         mockMvc.perform(get("/owners/*/pets/{petId}/visits/new", TEST_PET_ID))
    //             .andExpect(status().isOk())
    //             .andExpect(view().name("pets/createOrUpdateVisitForm"));
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O cenário da V2 representa o vínculo de Owner remoto com Pet local e a rota concreta.
    // ============================================================

    @Test
    public void testInitNewVisitForm() throws Exception {
        mockMvc.perform(get("/owners/1/pets/{petId}/visits/new", TEST_PET_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("pets/createOrUpdateVisitForm"));
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java: testProcessNewVisitFormSuccess
    // ============================================================
    // @Test
    //     public void testProcessNewVisitFormSuccess() throws Exception {
    //         mockMvc.perform(post("/owners/*/pets/{petId}/visits/new", TEST_PET_ID)
    //             .param("name", "George")
    //             .param("description", "Visit Description")
    //         )
    //             .andExpect(status().is3xxRedirection())
    //             .andExpect(view().name("redirect:/owners/{ownerId}"));
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O cenário da V2 representa o vínculo de Owner remoto com Pet local e a rota concreta.
    // ============================================================

    @Test
    public void testProcessNewVisitFormSuccess() throws Exception {
        mockMvc.perform(post("/owners/1/pets/{petId}/visits/new", TEST_PET_ID)
                .param("name", "George")
                .param("description", "Visit Description"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/owners/{ownerId}"));
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java: testProcessNewVisitFormHasErrors
    // ============================================================
    // @Test
    //     public void testProcessNewVisitFormHasErrors() throws Exception {
    //         mockMvc.perform(post("/owners/*/pets/{petId}/visits/new", TEST_PET_ID)
    //             .param("name", "George")
    //         )
    //             .andExpect(model().attributeHasErrors("visit"))
    //             .andExpect(status().isOk())
    //             .andExpect(view().name("pets/createOrUpdateVisitForm"));
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O cenário da V2 representa o vínculo de Owner remoto com Pet local e a rota concreta.
    // ============================================================

    @Test
    public void testProcessNewVisitFormHasErrors() throws Exception {
        mockMvc.perform(post("/owners/1/pets/{petId}/visits/new", TEST_PET_ID)
                .param("name", "George"))
                .andExpect(model().attributeHasErrors("visit"))
                .andExpect(status().isOk())
                .andExpect(view().name("pets/createOrUpdateVisitForm"));
    }

    @Test
    public void rejectsVisitForPetFromAnotherOwner() throws Exception {
        mockMvc.perform(get("/owners/2/pets/{petId}/visits/new", TEST_PET_ID))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/owners/2/pets/{petId}/visits/new", TEST_PET_ID)
                .param("description", "Wrong owner"))
                .andExpect(status().isNotFound());
    }

}
