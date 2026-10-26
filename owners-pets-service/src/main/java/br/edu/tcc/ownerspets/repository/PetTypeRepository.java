package br.edu.tcc.ownerspets.repository;

import br.edu.tcc.ownerspets.domain.PetType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetTypeRepository extends JpaRepository<PetType, Integer> {
    List<PetType> findAllByOrderByNameAsc();
}