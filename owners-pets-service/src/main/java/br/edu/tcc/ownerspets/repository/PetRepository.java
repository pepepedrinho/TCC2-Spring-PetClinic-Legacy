package br.edu.tcc.ownerspets.repository;

import br.edu.tcc.ownerspets.domain.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Integer> {
}