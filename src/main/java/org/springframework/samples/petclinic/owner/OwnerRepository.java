package org.springframework.samples.petclinic.owner;

import java.util.Collection;

import org.springframework.samples.petclinic.integration.OwnersClient;
import org.springframework.stereotype.Service;

/** HTTP adapter and local view composition, not an Owner persistence repository. */
@Service
public class OwnerRepository {
    private final OwnersClient ownersClient;

    private final PetRepository petRepository;

    public OwnerRepository(OwnersClient ownersClient, PetRepository petRepository) {
        this.ownersClient = ownersClient;
        this.petRepository = petRepository;
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java: findByLastName
    // ============================================================
    // /**
    //      * Retrieve {@link Owner}s from the data store by last name, returning all owners
    //      * whose last name <i>starts</i> with the given name.
    //      * @param lastName Value to search for
    //      * @return a Collection of matching {@link Owner}s (or an empty Collection if none
    //      * found)
    //      */
    //     @Query("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets WHERE owner.lastName LIKE :lastName%")
    //     @Transactional(readOnly = true)
    //     Collection<Owner> findByLastName(@Param("lastName") String lastName);
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // A persistência JPA local foi substituída por HTTP e composição com Pets locais.
    // ============================================================

    public Collection<Owner> findByLastName(String lastName) {
        Collection<Owner> owners = ownersClient.findByLastName(lastName);
        for (Owner owner : owners) {
            compose(owner);
        }
        return owners;
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java: findById
    // ============================================================
    // /**
    //      * Retrieve an {@link Owner} from the data store by id.
    //      * @param id the id to search for
    //      * @return the {@link Owner} if found
    //      */
    //     @Query("SELECT owner FROM Owner owner left join fetch owner.pets WHERE owner.id =:id")
    //     @Transactional(readOnly = true)
    //     Owner findById(@Param("id") Integer id);
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // A persistência JPA local foi substituída por HTTP e composição com Pets locais.
    // ============================================================

    public Owner findById(Integer id) {
        return compose(ownersClient.findById(id));
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java: save
    // ============================================================
    // /**
    //      * Save an {@link Owner} to the data store, either inserting or updating it.
    //      * @param owner the {@link Owner} to save
    //      */
    //     void save(Owner owner);
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // A persistência JPA local foi substituída por HTTP e composição com Pets locais.
    // ============================================================

    public void save(Owner owner) {
        owner.setId(ownersClient.save(owner).getId());
    }

    // Auxiliar novo da coexistência: une o Owner remoto aos Pets do banco legado.
    private Owner compose(Owner owner) {
        for (Pet pet : petRepository.findByOwnerId(owner.getId())) {
            owner.addPet(pet);
        }
        return owner;
    }
}
