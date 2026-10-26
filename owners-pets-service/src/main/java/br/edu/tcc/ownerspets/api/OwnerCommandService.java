package br.edu.tcc.ownerspets.api;

import br.edu.tcc.ownerspets.domain.Owner;
import br.edu.tcc.ownerspets.repository.OwnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class OwnerCommandService {

    private final OwnerRepository owners;

    public OwnerCommandService(OwnerRepository owners) {
        this.owners = owners;
    }

    @Transactional
    public Integer create(OwnerRequest request) {
        Owner owner = new Owner();
        updateFields(owner, request);
        return owners.save(owner).getId();
    }

    @Transactional
    public void update(Integer id, OwnerRequest request) {
        Owner owner = owners.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Proprietario nao encontrado"));

        updateFields(owner, request);
        owners.save(owner);
    }

    private void updateFields(Owner owner, OwnerRequest request) {
        owner.setFirstName(request.firstName());
        owner.setLastName(request.lastName());
        owner.setAddress(request.address());
        owner.setCity(request.city());
        owner.setTelephone(request.telephone());
    }
}