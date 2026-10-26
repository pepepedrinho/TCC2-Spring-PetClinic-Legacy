package br.edu.tcc.ownerspets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:ownerspets_api")
@AutoConfigureMockMvc
class OwnersPetsApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void deveConsultarProprietarioComSeusAnimais() throws Exception {
        mvc.perform(get("/api/owners/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.firstName").value("George"))
            .andExpect(jsonPath("$.lastName").value("Franklin"))
            .andExpect(jsonPath("$.pets[0].name").value("Leo"))
            .andExpect(jsonPath("$.pets[0].id").value(1));
    }

    @Test
    void devePesquisarPeloInicioDoSobrenome() throws Exception {
        mvc.perform(get("/api/owners").param("lastName", "Dav"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].lastName").value("Davis"))
            .andExpect(jsonPath("$[1].lastName").value("Davis"));
    }

    @Test
    void deveConsultarAnimal() throws Exception {
        mvc.perform(get("/api/pets/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Leo"))
            .andExpect(jsonPath("$.ownerId").value(1))
            .andExpect(jsonPath("$.typeName").value("cat"));
    }

    @Test
    void deveListarTipos() throws Exception {
        mvc.perform(get("/api/pet-types"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void deveRetornar404ParaIdsInexistentes() throws Exception {
        mvc.perform(get("/api/owners/9999"))
            .andExpect(status().isNotFound());

        mvc.perform(get("/api/pets/9999"))
            .andExpect(status().isNotFound());
    }
}