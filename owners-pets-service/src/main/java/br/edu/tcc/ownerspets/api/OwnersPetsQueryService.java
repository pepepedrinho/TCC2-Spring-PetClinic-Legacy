package br.edu.tcc.ownerspets.api;

import br.edu.tcc.ownerspets.domain.Owner;
import br.edu.tcc.ownerspets.domain.Pet;
import br.edu.tcc.ownerspets.domain.PetType;
import br.edu.tcc.ownerspets.repository.OwnerRepository;
import br.edu.tcc.ownerspets.repository.PetRepository;
import br.edu.tcc.ownerspets.repository.PetTypeRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OwnersPetsQueryService {

    private final OwnerRepository owners;
    private final PetRepository pets;
    private final PetTypeRepository types;

    public OwnersPetsQueryService(
            OwnerRepository owners,
            PetRepository pets,
            PetTypeRepository types) {
        this.owners = owners;
        this.pets = pets;
        this.types = types;
    }

    public OwnerDto findOwner(Integer id) {
        return owners.findWithPetsById(id)
                .map(this::toOwnerDto)
                .orElse(null);
    }

    public List<OwnerDto> searchOwners(String lastName) {
        return owners
                .findByLastNameStartingWithIgnoreCaseOrderByLastNameAscFirstNameAsc(lastName)
                .stream()
                .map(this::toOwnerDto)
                .toList();
    }

    public PetDto findPet(Integer id) {
        return pets.findById(id).map(this::toPetDto).orElse(null);
    }

    public List<PetTypeDto> findTypes() {
        return types.findAllByOrderByNameAsc().stream()
                .map(t -> new PetTypeDto(t.getId(), t.getName()))
                .toList();
    }

    private OwnerDto toOwnerDto(Owner owner) {
        List<PetDto> ownerPets = owner.getPets().stream()
                .sorted(Comparator.comparing(Pet::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toPetDto)
                .toList();

        return new OwnerDto(
                owner.getId(),
                owner.getFirstName(),
                owner.getLastName(),
                owner.getAddress(),
                owner.getCity(),
                owner.getTelephone(),
                ownerPets);
    }

    private PetDto toPetDto(Pet pet) {
        return new PetDto(
                pet.getId(),
                pet.getName(),
                pet.getBirthDate(),
                pet.getType().getId(),
                pet.getType().getName(),
                pet.getOwner().getId());
    }

    public record OwnerDto(
            Integer id, String firstName, String lastName,
            String address, String city, String telephone,
            List<PetDto> pets) {}

    public record PetDto(
            Integer id, String name, LocalDate birthDate,
            Integer typeId, String typeName, Integer ownerId) {}

    public record PetTypeDto(Integer id, String name) {}
}