package org.springframework.samples.petclinic.owner;

import java.util.Collection;

/** Contrato de acesso a Owners para o código legado. */
public interface OwnerRepository {
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
    // A implementação via HTTP e a composição com Pets locais ficam em OwnerIntegrationAdapter.
    // ============================================================

    Collection<Owner> findByLastName(String lastName);

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
    // A implementação via HTTP e a composição com Pets locais ficam em OwnerIntegrationAdapter.
    // ============================================================

    Owner findById(Integer id);

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
    // A persistência via HTTP fica em OwnerIntegrationAdapter.
    // ============================================================

    void save(Owner owner);
}
