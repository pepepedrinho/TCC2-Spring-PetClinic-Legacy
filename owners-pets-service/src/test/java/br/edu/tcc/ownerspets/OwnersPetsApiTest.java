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

    @Test
    void deveCadastrarProprietario() throws Exception {
        String json = """
            {"firstName":"Ana","lastName":"Silva",
             "address":"Rua A","city":"Curitiba",
             "telephone":"4199999999"}
            """;

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/owners")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/owners/11"));

        mvc.perform(get("/api/owners/11"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Ana"));
    }

    @Test
    void deveAtualizarProprietario() throws Exception {
        String json = """
            {"firstName":"George","lastName":"Franklin",
             "address":"Endereco atualizado","city":"Madison",
             "telephone":"6085551023"}
            """;

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/owners/1")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/owners/1"))
            .andExpect(jsonPath("$.address").value("Endereco atualizado"));
    }

    @Test
    void deveRejeitarCadastroInvalido() throws Exception {
        String json = """
            {"firstName":"","lastName":"",
             "address":"","city":"",
             "telephone":"abc"}
            """;

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/owners")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());
    }
    @Test
    void deveCadastrarAnimal() throws Exception {
        String json = """
            {"name":"Thor","birthDate":"2020-05-15","typeId":2}
            """;

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/owners/1/pets")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/pets/14"));

        mvc.perform(get("/api/pets/14"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Thor"))
            .andExpect(jsonPath("$.ownerId").value(1))
            .andExpect(jsonPath("$.typeName").value("dog"));
    }

    @Test
    void deveAtualizarAnimal() throws Exception {
        String json = """
            {"name":"Sly Atualizado","birthDate":"2012-06-08","typeId":2}
            """;

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/pets/13")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/pets/13"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Sly Atualizado"))
            .andExpect(jsonPath("$.typeName").value("dog"))
            .andExpect(jsonPath("$.ownerId").value(10));
    }

    @Test
    void deveRejeitarNomeDuplicadoNoMesmoProprietario() throws Exception {
        String json = """
            {"name":"Leo","birthDate":"2020-05-15","typeId":1}
            """;

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/owners/1/pets")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isConflict());
    }

    @Test
    void deveRejeitarReferenciasInvalidas() throws Exception {
        String json = """
            {"name":"Animal Teste","birthDate":"2020-05-15","typeId":1}
            """;

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/owners/9999/pets")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isNotFound());

        String tipoInvalido = """
            {"name":"Animal Teste","birthDate":"2020-05-15","typeId":9999}
            """;

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/owners/1/pets")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(tipoInvalido))
            .andExpect(status().isBadRequest());
    }}