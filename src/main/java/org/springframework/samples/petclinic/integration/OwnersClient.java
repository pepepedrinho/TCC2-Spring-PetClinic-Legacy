package org.springframework.samples.petclinic.integration;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** One-way HTTP boundary. Never sends Pets or Visits to Owners. */
// Componente novo da extração de Owner: fronteira HTTP do legado com o serviço; envia somente dados de
// Owner e delimita falhas remotas para o tratamento central.
@Component
public class OwnersClient {
    private final String url;

    private final RestTemplate restTemplate;

    public OwnersClient(@Value("${owners.base-url}") String url) {
        this.url = url + "/api/owners";
        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setConnectTimeout(1500);
        simpleClientHttpRequestFactory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(simpleClientHttpRequestFactory);
    }

    // Antes, translate decidia o status aqui; as capturas restritas agora delegam ao handler central.
    public Collection<Owner> findByLastName(String name) {
        java.net.URI uri = UriComponentsBuilder.fromHttpUrl(url).queryParam("lastName", name).build().encode().toUri();
        try {
            return Arrays.asList(restTemplate.getForObject(uri, Owner[].class));
        } catch (RestClientException | HttpMessageConversionException ex) {
            throw IntegrationErrorHandler.remoteFailure(ex);
        }
    }

    // A chamada HTTP permanece aqui; a classificação antes local foi centralizada no handler.
    public Owner findById(Integer id) {
        try {
            return restTemplate.getForObject(url + "/" + id, Owner.class);
        } catch (RestClientException | HttpMessageConversionException ex) {
            throw IntegrationErrorHandler.remoteFailure(ex);
        }
    }

    // Payload e verbos preservados; as falhas antes traduzidas localmente seguem ao handler central.
    public Owner save(Owner owner) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("firstName", owner.getFirstName());
        body.put("lastName", owner.getLastName());
        body.put("address", owner.getAddress());
        body.put("city", owner.getCity());
        body.put("telephone", owner.getTelephone());
        try {
            if (owner.isNew()) {
                return restTemplate.postForObject(url, body, Owner.class);
            }
            restTemplate.put(url + "/" + owner.getId(), body);
            return owner;
        } catch (RestClientException | HttpMessageConversionException ex) {
            throw IntegrationErrorHandler.remoteFailure(ex);
        }
    }
}
