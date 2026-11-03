package br.edu.tcc.owners;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;

import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Componente novo da extração: expõe o contrato REST de Owner. Não é uma transferência dos métodos MVC de
// formulários do monólito.
@RestController
@RequestMapping("/api/owners")
public class OwnerController {

    private final OwnerRepository ownerRepository;

    public OwnerController(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    @GetMapping
    public List<Owner> search(@RequestParam(defaultValue = "") String lastName) {
        return ownerRepository.findByLastNameStartingWithIgnoreCaseOrderByLastNameAscIdAsc(lastName);
    }

    @GetMapping("/{id}")
    public Owner get(@PathVariable Integer id) {
        return ownerRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<Owner> create(@Valid @RequestBody Owner owner) {
        Owner newOwner = new Owner();
        newOwner.updateFrom(owner);
        Owner savedOwner = ownerRepository.save(newOwner);
        return ResponseEntity.created(URI.create("/api/owners/" + savedOwner.getId())).body(savedOwner);
    }

    @PutMapping("/{id}")
    @Transactional
    public Owner update(@PathVariable Integer id, @Valid @RequestBody Owner input) {
        Owner owner = get(id);
        owner.updateFrom(input);
        return ownerRepository.save(owner);
    }

    /** No deletion, even for currently childless owners: no distributed race or callback. */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        get(id);
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Owner deletion is disabled to preserve legacy references");
    }
}
