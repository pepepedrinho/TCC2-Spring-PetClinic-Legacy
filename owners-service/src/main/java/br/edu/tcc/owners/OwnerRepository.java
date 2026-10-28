package br.edu.tcc.owners;
import java.util.List;
public interface OwnerRepository extends org.springframework.data.jpa.repository.JpaRepository<Owner,Integer> {
    List<Owner> findByLastNameStartingWithIgnoreCaseOrderByLastNameAscIdAsc(String lastName);
}
