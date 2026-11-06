
package org.springframework.samples.petclinic.owner;

import java.util.Collection;

// Originalmente, o Spring Data JPA implementava a persistência local.
// Agora, a implementação é realizada por OwnerIntegrationAdapter via HTTP.
public interface OwnerRepository {

    /**
     * Retrieve {@link Owner}s from the data store by last name, returning all owners
     * whose last name <i>starts</i> with the given name.
     * @param lastName Value to search for
     * @return a Collection of matching {@link Owner}s (or an empty Collection if none
     * found)
     */


    // @Query("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets WHERE owner.lastName LIKE :lastName%")
    // @Transactional(readOnly = true)
    // Collection<Owner> findByLastName(@Param("lastName") String lastName);

    Collection<Owner> findByLastName(String lastName);

    /**
     * Retrieve an {@link Owner} from the data store by id.
     * @param id the id to search for
     * @return the {@link Owner} if found
     */

    // @Query("SELECT owner FROM Owner owner left join fetch owner.pets WHERE owner.id =:id")
    // @Transactional(readOnly = true)
    // Owner findById(@Param("id") Integer id);

    Owner findById(Integer id);

    /**
     * Save an {@link Owner} to the data store, either inserting or updating it.
     * @param owner the {@link Owner} to save
     */

    void save(Owner owner);
}
