package br.edu.tcc.ownerspets.repository;

import br.edu.tcc.ownerspets.domain.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Integer> {
    List<Pet> findByOwnerIdAndNameIgnoreCase(Integer ownerId, String name);
}