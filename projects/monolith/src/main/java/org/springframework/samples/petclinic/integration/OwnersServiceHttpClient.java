package org.springframework.samples.petclinic.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Reads Owner data for the Pet and Visit workflows that remain in the monolith. */
@Service
public class OwnersServiceHttpClient {
    private final String url;
    private final RestTemplate restTemplate;

    public OwnersServiceHttpClient(@Value("${owners.base-url}") String url) {
        this.url = url + "/api/owners/{id}";
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1500);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
    }

    public Owner findById(Integer id) {
        try {
            Owner owner = restTemplate.getForObject(url, Owner.class, id);
            if (owner == null || !id.equals(owner.getId())) {
                throw new RestClientException("Invalid Owner response");
            }
            return owner;
        } catch (RestClientException | HttpMessageConversionException ex) {
            throw IntegrationErrorHandler.remoteFailure(ex);
        }
    }
}
