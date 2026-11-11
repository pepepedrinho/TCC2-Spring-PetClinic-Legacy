package br.edu.tcc.integration;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.edu.tcc.owners.Owner;
import br.edu.tcc.owners.OwnerLookup;
import br.edu.tcc.owners.OwnerRepository;

@RestController
public class OwnerRestController {

    private final OwnerRepository ownerRepository;
    private final OwnerLookup ownerLookup;

    public OwnerRestController(OwnerRepository ownerRepository, OwnerLookup ownerLookup) {
        this.ownerRepository = ownerRepository;
        this.ownerLookup = ownerLookup;
    }

    @GetMapping("/api/owners")
    public List<Owner> search(@RequestParam(defaultValue = "") String lastName) {
        return ownerRepository.findByLastName(lastName);
    }

    @GetMapping("/api/owners/{id}")
    public Owner get(@PathVariable Integer id) {
        return ownerLookup.findOwner(id);
    }

    @PostMapping("/api/owners")
    public ResponseEntity<Owner> create(@Valid @RequestBody Owner owner) {
        Owner newOwner = new Owner();
        newOwner.updateFrom(owner);
        Owner savedOwner = ownerRepository.save(newOwner);
        return ResponseEntity.created(URI.create("/api/owners/" + savedOwner.getId())).body(savedOwner);
    }

    @PutMapping("/api/owners/{id}")
    @Transactional
    public Owner update(@PathVariable Integer id, @Valid @RequestBody Owner input) {
        Owner owner = ownerLookup.findOwner(id);
        owner.updateFrom(input);
        return ownerRepository.save(owner);
    }

    /** No deletion, even for currently childless owners: no distributed race or callback. */
    @DeleteMapping("/api/owners/{id}")
    public void delete(@PathVariable Integer id) {
        ownerLookup.findOwner(id);
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Owner deletion is disabled to preserve legacy references");
    }
}
