package br.edu.tcc.ownerspets.repository;

import br.edu.tcc.ownerspets.domain.Owner;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerRepository extends JpaRepository<Owner, Integer> {

    @EntityGraph(attributePaths = {"pets", "pets.type"})
    Optional<Owner> findWithPetsById(Integer id);

    List<Owner> findByLastNameStartingWithIgnoreCaseOrderByLastNameAscFirstNameAsc(String lastName);
}