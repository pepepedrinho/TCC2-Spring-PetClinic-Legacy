package br.edu.tcc.owners;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestClient;

import br.edu.tcc.integration.LegacyPetsHttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:owner-web-tests;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class OwnerWebTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    LegacyPetsHttpClient pets;

    @Test
    void searchesByPrefixAndReadsPetsOnceForMultipleOwners() throws Exception {
        when(pets.findPets(List.of(2, 4))).thenReturn(Map.of(
                2, List.of(new LegacyPetsHttpClient.PetSummary(4, "Test Cat", null, "cat", List.of())),
                4, List.of()));

        mvc.perform(get("/owners/find")).andExpect(status().isOk())
                .andExpect(content().string(containsString("search-owner-form")));
        mvc.perform(get("/owners").param("lastName", "frank")).andExpect(redirectedUrl("/owners/1"));
        mvc.perform(get("/owners").param("lastName", "Unknown"))
                .andExpect(content().string(containsString("not found")));
        mvc.perform(get("/owners").param("lastName", "Dav")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Betty Davis")))
                .andExpect(content().string(containsString("Test Cat")));
        verify(pets).findPets(List.of(2, 4));
        verifyNoMoreInteractions(pets);
    }

    @Test
    void rendersVisitsAndLinksToLegacyPetPages() throws Exception {
        var visit = new LegacyPetsHttpClient.VisitSummary(LocalDate.of(2020, 2, 29), "Annual exam");
        var pet = new LegacyPetsHttpClient.PetSummary(1, "Leo", LocalDate.of(2018, 1, 1), "cat", List.of(visit));
        when(pets.findPets(List.of(1))).thenReturn(Map.of(1, List.of(pet)));

        mvc.perform(get("/owners/1")).andExpect(status().isOk())
                .andExpect(content().string(containsString("George Franklin")))
                .andExpect(content().string(containsString("Annual exam")))
                .andExpect(content().string(containsString("2020-02-29")))
                .andExpect(content().string(containsString("/owners/1/pets/1/visits/new")));
        mvc.perform(get("/owners/9999")).andExpect(status().isNotFound());
        verify(pets).findPets(List.of(1));
        verifyNoMoreInteractions(pets);
    }

    @Test
    void validatesAndPersistsFormsWithoutCallingLegacy() throws Exception {
        mvc.perform(get("/owners/new")).andExpect(content().string(containsString("Add Owner")));
        mvc.perform(post("/owners/new").param("firstName", "Ana"))
                .andExpect(model().attributeHasFieldErrors("owner", "lastName", "address", "city", "telephone"))
                .andExpect(content().string(containsString("has-error")));

        String location = mvc.perform(ownerForm("/owners/new", "Recife").param("id", "1"))
                .andExpect(status().isFound()).andReturn().getResponse().getRedirectedUrl();
        assertThat(location).isNotEqualTo("/owners/1");
        mvc.perform(get(location + "/edit")).andExpect(content().string(containsString("Update Owner")));
        mvc.perform(ownerForm(location + "/edit", ""))
                .andExpect(model().attributeHasFieldErrors("owner", "city"))
                .andExpect(content().string(containsString("Update Owner")));
        mvc.perform(get("/api" + location)).andExpect(jsonPath("$.city").value("Recife"));
        mvc.perform(ownerForm(location + "/edit", "Olinda").param("id", "1"))
                .andExpect(redirectedUrl(location));
        mvc.perform(get("/api" + location)).andExpect(jsonPath("$.city").value("Olinda"));
        mvc.perform(get("/api/owners/1")).andExpect(jsonPath("$.firstName").value("George"));
        mvc.perform(ownerForm("/owners/9999/edit", "Olinda")).andExpect(status().isNotFound());
        verifyNoInteractions(pets);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"not-json", "{}"})
    void legacyFailureAffectsOnlyPagesThatNeedPets(String legacyBody) throws Exception {
        var unavailableLegacy = new LegacyPetsHttpClient(RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    if (legacyBody == null) {
                        throw new IOException("Legacy connection unavailable");
                    }
                    var response = new MockClientHttpResponse(legacyBody.getBytes(StandardCharsets.UTF_8), HttpStatus.OK);
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    return response;
                }), "http://localhost:8080");
        when(pets.findPets(List.of(1))).thenAnswer(call -> unavailableLegacy.findPets(call.getArgument(0)));

        mvc.perform(get("/owners/1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(status().reason("Pets and visits are unavailable"));
        mvc.perform(get("/owners/1/edit")).andExpect(status().isOk());
        mvc.perform(get("/api/owners/1")).andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder ownerForm(String path, String city) {
        return post(path).param("firstName", "Ana").param("lastName", "Test Web")
                .param("address", "Rua A").param("city", city).param("telephone", "1234567890");
    }
}
