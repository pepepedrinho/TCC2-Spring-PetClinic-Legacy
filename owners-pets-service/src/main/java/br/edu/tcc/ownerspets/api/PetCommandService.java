package br.edu.tcc.ownerspets.api;

import br.edu.tcc.ownerspets.domain.Owner;
import br.edu.tcc.ownerspets.domain.Pet;
import br.edu.tcc.ownerspets.domain.PetType;
import br.edu.tcc.ownerspets.repository.OwnerRepository;
import br.edu.tcc.ownerspets.repository.PetRepository;
import br.edu.tcc.ownerspets.repository.PetTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PetCommandService {

    private final PetRepository pets;
    private final OwnerRepository owners;
    private final PetTypeRepository types;

    public PetCommandService(PetRepository pets,
                             OwnerRepository owners,
                             PetTypeRepository types) {
        this.pets = pets;
        this.owners = owners;
        this.types = types;
    }

    @Transactional
    public Integer create(Integer ownerId, PetRequest request) {
        Owner owner = findOwner(ownerId);

        validateName(ownerId, request.name(), null);

        Pet pet = new Pet();
        pet.setOwner(owner);
        updateFields(pet, request);

        return pets.saveAndFlush(pet).getId();
    }

    @Transactional
    public void update(Integer petId, PetRequest request) {
        Pet pet = pets.findById(petId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Animal nao encontrado"));

        validateName(pet.getOwner().getId(), request.name(), petId);

        updateFields(pet, request);
        pets.save(pet);
    }

    private Owner findOwner(Integer ownerId) {
        return owners.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Proprietario nao encontrado"));
    }

    private void updateFields(Pet pet, PetRequest request) {
        PetType type = types.findById(request.typeId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Tipo de animal invalido"));

        pet.setName(request.name());
        pet.setBirthDate(request.birthDate());
        pet.setType(type);
    }

    private void validateName(Integer ownerId, String name, Integer currentPetId) {
        boolean duplicate = pets.findByOwnerIdAndNameIgnoreCase(ownerId, name)
                .stream()
                .anyMatch(pet -> !pet.getId().equals(currentPetId));

        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este proprietario ja possui um animal com esse nome");
        }
    }
}