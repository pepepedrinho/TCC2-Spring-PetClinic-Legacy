package org.springframework.samples.petclinic.owner;

import java.util.Collection;
import org.springframework.stereotype.Service;
import org.springframework.samples.petclinic.integration.OwnersClient;

/** HTTP adapter and local view composition, not an Owner persistence repository. */
@Service
public class OwnerRepository {
    private final OwnersClient client;
    private final PetRepository pets;
    public OwnerRepository(OwnersClient client, PetRepository pets) {
        this.client = client;
        this.pets = pets;
    }
    public Collection<Owner> findByLastName(String lastName) {
        Collection<Owner> owners = client.findByLastName(lastName);
        for (Owner owner : owners) { compose(owner); }
        return owners;
    }
    public Owner findById(Integer id) { return compose(client.findById(id)); }
    public void save(Owner owner) { owner.setId(client.save(owner).getId()); }
    private Owner compose(Owner owner) {
        for (Pet pet : pets.findByOwnerId(owner.getId())) { owner.addPet(pet); }
        return owner;
    }
}
