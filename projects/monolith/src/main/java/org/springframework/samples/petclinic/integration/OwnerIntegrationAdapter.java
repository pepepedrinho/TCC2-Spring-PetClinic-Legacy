package org.springframework.samples.petclinic.integration;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** One-way HTTP boundary. Never sends Pets or Visits to Owners. */
// Componente novo da extração de Owner: fronteira HTTP do legado com o serviço; envia somente dados de
// Owner e delimita falhas remotas para o tratamento central.
@Service
public class OwnerIntegrationAdapter implements OwnerRepository {
    private final String url;

    private final RestTemplate restTemplate;

    private final PetRepository petRepository;

    public OwnerIntegrationAdapter(@Value("${owners.base-url}") String url, PetRepository petRepository) {
        this.petRepository = petRepository;
        this.url = url + "/api/owners";
        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setConnectTimeout(1500);
        simpleClientHttpRequestFactory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(simpleClientHttpRequestFactory);
    }

    @Override
    public Collection<Owner> findByLastName(String lastName) {
        URI uri = UriComponentsBuilder.fromHttpUrl(url).queryParam("lastName", lastName).build().encode().toUri();
        Collection<Owner> owners;
        try {
            owners = Arrays.asList(restTemplate.getForObject(uri, Owner[].class));
        } catch (RestClientException | HttpMessageConversionException ex) {
            throw IntegrationErrorHandler.remoteFailure(ex);
        }
        for (Owner owner : owners) {
            compose(owner);
        }
        return owners;
    }

    // A chamada HTTP permanece aqui; a classificação antes local foi centralizada no handler.
    @Override
    public Owner findById(Integer id) {
        Owner owner;
        try {
            owner = restTemplate.getForObject(url + "/" + id, Owner.class);
        } catch (RestClientException | HttpMessageConversionException ex) {
            throw IntegrationErrorHandler.remoteFailure(ex);
        }
        return compose(owner);
    }

    // Payload e verbos preservados; as falhas antes traduzidas localmente seguem ao handler central.
    @Override
    public void save(Owner owner) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("firstName", owner.getFirstName());
        body.put("lastName", owner.getLastName());
        body.put("address", owner.getAddress());
        body.put("city", owner.getCity());
        body.put("telephone", owner.getTelephone());
        try {
            if (owner.isNew()) {
                Owner savedOwner = restTemplate.postForObject(url, body, Owner.class);
                owner.setId(savedOwner.getId());
            } else {
                restTemplate.put(url + "/" + owner.getId(), body);
            }
        } catch (RestClientException | HttpMessageConversionException ex) {
            throw IntegrationErrorHandler.remoteFailure(ex);
        }
    }

    // Auxiliar novo da coexistência: une o Owner remoto aos Pets do banco legado.
    private Owner compose(Owner owner) {
        for (Pet pet : petRepository.findByOwnerId(owner.getId())) {
            owner.addPet(pet);
        }
        return owner;
    }
}
