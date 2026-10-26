package org.springframework.samples.petclinic.owner;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.samples.petclinic.integration.OwnersPetsClient;
import org.springframework.samples.petclinic.integration.OwnersPetsClient.OwnerResponse;
import org.springframework.samples.petclinic.integration.OwnersPetsClient.PetResponse;
import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.samples.petclinic.visit.VisitRepository;
import org.springframework.stereotype.Component;

@Component
public class RemoteOwnerAssembler {

    private final OwnersPetsClient client;
    private final VisitRepository visits;

    public RemoteOwnerAssembler(OwnersPetsClient client,
                                VisitRepository visits) {
        this.client = client;
        this.visits = visits;
    }

    public Owner findById(int ownerId) {
        OwnerResponse response = client.findOwnerById(ownerId);

        if (response == null) {
            return null;
        }

        Owner owner = new Owner();
        owner.setId(response.getId());
        owner.setFirstName(response.getFirstName());
        owner.setLastName(response.getLastName());
        owner.setAddress(response.getAddress());
        owner.setCity(response.getCity());
        owner.setTelephone(response.getTelephone());

        Set<Pet> ownerPets = new HashSet<>();

        if (response.getPets() != null) {
            for (PetResponse petResponse : response.getPets()) {
                Pet pet = new Pet();
                pet.setId(petResponse.getId());
                pet.setName(petResponse.getName());

                if (petResponse.getBirthDate() != null) {
                    pet.setBirthDate(Date.valueOf(
                            LocalDate.parse(petResponse.getBirthDate())));
                }

                PetType type = new PetType();
                type.setId(petResponse.getTypeId());
                type.setName(petResponse.getTypeName());

                pet.setType(type);
                pet.setOwner(owner);

                Set<Visit> petVisits = new LinkedHashSet<>(
                        visits.findByPetId(pet.getId()));
                pet.setVisitsInternal(petVisits);

                ownerPets.add(pet);
            }
        }

        owner.setPetsInternal(ownerPets);
        return owner;
    }
}