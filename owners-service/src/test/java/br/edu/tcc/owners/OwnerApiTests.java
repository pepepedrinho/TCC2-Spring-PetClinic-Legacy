package br.edu.tcc.owners;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Teste novo da extração: verifica o contrato REST, validação e proibição de exclusão para preservar
// referências do legado.
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:owners-tests;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class OwnerApiTests {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    OwnerRepository ownerRepository;

    @Test
    void historicalOwnerAndPrefixSearch() throws Exception {
        mockMvc.perform(get("/api/owners/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Franklin"));
        mockMvc.perform(get("/api/owners?lastName=dav")).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void createAndUpdate() throws Exception {
        String body = "{\"id\":1,\"firstName\":\"Ana\",\"lastName\":\"Novo\","
                + "\"address\":\"Rua A\",\"city\":\"Recife\",\"telephone\":\"1234567890\"}";
        String json = mockMvc.perform(post("/api/owners").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("id").asInt();
        org.junit.jupiter.api.Assertions.assertTrue(id > 10);
        mockMvc.perform(put("/api/owners/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("Novo", "Atualizado")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lastName").value("Atualizado"));
        mockMvc.perform(get("/api/owners/1")).andExpect(jsonPath("$.firstName").value("George"));
    }

    @Test
    void rejectsInvalidOwner() throws Exception {
        mockMvc.perform(post("/api/owners").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownOwner() throws Exception {
        mockMvc.perform(get("/api/owners/999999")).andExpect(status().isNotFound());
    }

    @Test
    void deletionCannotOrphanPets() throws Exception {
        mockMvc.perform(delete("/api/owners/1")).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/owners/999999")).andExpect(status().isNotFound());
    }

    @Test
    void noPetBusinessEndpoints() throws Exception {
        mockMvc.perform(get("/api/pets/1")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/pet-types")).andExpect(status().isNotFound());
    }
}
