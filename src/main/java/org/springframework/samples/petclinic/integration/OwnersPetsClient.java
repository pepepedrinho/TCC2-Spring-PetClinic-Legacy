package org.springframework.samples.petclinic.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@Component
public class OwnersPetsClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OwnersPetsClient(
            @Value("${owners-pets.service.url:http://localhost:8082}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
    }

    public OwnerResponse findOwnerById(int ownerId) {
        return restTemplate.getForObject(
                baseUrl + "/api/owners/{id}",
                OwnerResponse.class,
                ownerId
        );
    }


    public OwnerResponse[] findOwnersByLastName(String lastName) {
        return restTemplate.getForObject(
                baseUrl + "/api/owners?lastName={lastName}",
                OwnerResponse[].class,
                lastName
        );
    }
    public int createOwner(OwnerRequest request) {
        org.springframework.http.ResponseEntity<Void> response =
                restTemplate.postForEntity(
                        baseUrl + "/api/owners",
                        request,
                        Void.class);

        java.net.URI location = response.getHeaders().getLocation();

        if (location == null) {
            throw new IllegalStateException("API nao retornou Location");
        }

        String path = location.getPath();
        return Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
    }

    public void updateOwner(int ownerId, OwnerRequest request) {
        restTemplate.put(
                baseUrl + "/api/owners/{id}",
                request,
                ownerId);
    }    public PetTypeResponse[] findPetTypes() {
        return restTemplate.getForObject(
                baseUrl + "/api/pet-types",
                PetTypeResponse[].class);
    }

    public static class PetTypeResponse {
        private Integer id;
        private String name;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    public PetResponse findPetById(int petId) {
        return restTemplate.getForObject(
                baseUrl + "/api/pets/{id}",
                PetResponse.class,
                petId);
    }

    public int createPet(int ownerId, PetRequest request) {
        org.springframework.http.ResponseEntity<Void> response =
                restTemplate.postForEntity(
                        baseUrl + "/api/owners/{ownerId}/pets",
                        request,
                        Void.class,
                        ownerId);

        java.net.URI location = response.getHeaders().getLocation();

        if (location == null) {
            throw new IllegalStateException("API nao retornou Location");
        }

        String path = location.getPath();
        return Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
    }

    public void updatePet(int petId, PetRequest request) {
        restTemplate.put(
                baseUrl + "/api/pets/{id}",
                request,
                petId);
    }
    public static class OwnerResponse {
        private Integer id;
        private String firstName;
        private String lastName;
        private String address;
        private String city;
        private String telephone;
        private List<PetResponse> pets;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public List<PetResponse> getPets() { return pets; }
        public void setPets(List<PetResponse> pets) { this.pets = pets; }

        public String getTelephone() { return telephone; }
        public void setTelephone(String telephone) { this.telephone = telephone; }
    }

    public static class PetResponse {
        private Integer id;
        private String name;
        private String birthDate;
        private Integer typeId;
        private String typeName;
        private Integer ownerId;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getBirthDate() { return birthDate; }
        public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

        public Integer getTypeId() { return typeId; }
        public void setTypeId(Integer typeId) { this.typeId = typeId; }

        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }

        public Integer getOwnerId() { return ownerId; }
        public void setOwnerId(Integer ownerId) { this.ownerId = ownerId; }
    }}