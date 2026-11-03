package br.edu.tcc.owners;

import java.util.List;

// Componente novo da extração: usa JpaRepository para persistir Owner no banco do serviço, sem
// relacionamentos JPA com Pets do legado.
public interface OwnerRepository extends org.springframework.data.jpa.repository.JpaRepository<Owner, Integer> {
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java: findByLastName
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
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // A busca persiste no serviço; a V2 já usa prefixo sem distinguir maiúsculas e ordenação explícita.
    // Não é uma tradução literal da JPQL original.

    List<Owner> findByLastNameStartingWithIgnoreCaseOrderByLastNameAscIdAsc(String lastName);
}
