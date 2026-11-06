package br.edu.tcc.owners;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

// Componente novo da extração: usa JpaRepository para persistir Owner no banco do serviço, sem
// relacionamentos JPA com Pets do legado. Porém, a integração continua sendo feita no monólito
// pelo método compose(), que utiliza o ownerId armazenado em cada Pet para recuperar os animais
// locais e associá-los ao respectivo Owner recebido do microsserviço.
public interface OwnerRepository extends JpaRepository<Owner, Integer> {

    // Poderia ser utilizado o método de consulta derivada do Spring Data JPA:
    // List<Owner> findByLastNameStartingWithIgnoreCaseOrderByLastNameAscIdAsc(String lastName);

    @Transactional(readOnly = true)
    @Query("SELECT o FROM Owner o " +
        "WHERE LOWER(o.lastName) LIKE LOWER(CONCAT(:lastName, '%')) " +
        "ORDER BY o.lastName ASC, o.id ASC")
    List<Owner> findByLastName(@Param("lastName") String lastName);
}
