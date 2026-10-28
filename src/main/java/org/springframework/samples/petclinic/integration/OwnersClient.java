package org.springframework.samples.petclinic.integration;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** One-way HTTP boundary. Never sends Pets or Visits to Owners. */
@Component
public class OwnersClient {
    private final RestTemplate http;
    private final String url;
    public OwnersClient(@Value("${owners.base-url}") String url) {
        this.url = url + "/api/owners";
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1500);
        factory.setReadTimeout(3000);
        this.http = new RestTemplate(factory);
    }
    public Collection<Owner> findByLastName(String name) {
        java.net.URI uri = UriComponentsBuilder.fromHttpUrl(url).queryParam("lastName", name).build().encode().toUri();
        try { return Arrays.asList(http.getForObject(uri, Owner[].class)); }
        catch (RestClientException | HttpMessageConversionException ex) { throw translate(ex); }
    }
    public Owner findById(Integer id) {
        try { return http.getForObject(url + "/" + id, Owner.class); }
        catch (RestClientException | HttpMessageConversionException ex) { throw translate(ex); }
    }
    public Owner save(Owner owner) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("firstName", owner.getFirstName()); body.put("lastName", owner.getLastName());
        body.put("address", owner.getAddress()); body.put("city", owner.getCity());
        body.put("telephone", owner.getTelephone());
        try {
            if (owner.isNew()) { return http.postForObject(url, body, Owner.class); }
            http.put(url + "/" + owner.getId(), body);
            return owner;
        } catch (RestClientException | HttpMessageConversionException ex) { throw translate(ex); }
    }
    private RuntimeException translate(RuntimeException ex) {
        if (ex instanceof HttpStatusCodeException) {
            HttpStatus status = ((HttpStatusCodeException) ex).getStatusCode();
            if (status == HttpStatus.NOT_FOUND) { return new OwnerNotFoundException(); }
            if (status == HttpStatus.BAD_REQUEST) { return new OwnerValidationException(); }
        }
        return new OwnersUnavailableException(ex);
    }
}
