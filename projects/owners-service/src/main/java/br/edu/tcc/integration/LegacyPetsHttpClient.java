package br.edu.tcc.integration;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LegacyPetsHttpClient {

    private final RestClient client;

    public LegacyPetsHttpClient(RestClient.Builder builder, @Value("${legacy.base-url}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1500);
        factory.setReadTimeout(3000);
        this.client = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    public Map<Integer, List<PetSummary>> findPets(List<Integer> ownerIds) {
        try {
            Map<Integer, List<PetSummary>> result = client.post().uri("/internal/owner-pets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ownerIds)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (result == null || !result.keySet().containsAll(ownerIds)) {
                throw new RestClientException("Incomplete legacy Pet response");
            }
            return result;
        } catch (RestClientException | HttpMessageConversionException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Pets and visits are unavailable", ex);
        }
    }

    public record PetSummary(Integer id, String name, LocalDate birthDate, String type, List<VisitSummary> visits) {}

    public record VisitSummary(LocalDate date, String description) {}
}
