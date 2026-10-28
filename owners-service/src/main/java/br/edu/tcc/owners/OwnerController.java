package br.edu.tcc.owners;
import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
@RestController
@RequestMapping("/api/owners")
public class OwnerController {
    private final OwnerRepository owners;
    public OwnerController(OwnerRepository owners) { this.owners=owners; }
    @GetMapping public List<Owner> search(@RequestParam(defaultValue="") String lastName) {
        return owners.findByLastNameStartingWithIgnoreCaseOrderByLastNameAscIdAsc(lastName);
    }
    @GetMapping("/{id}") public Owner get(@PathVariable Integer id) {
        return owners.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @PostMapping public ResponseEntity<Owner> create(@Valid @RequestBody Owner owner) {
        Owner fresh = new Owner(); fresh.updateFrom(owner);
        Owner saved=owners.save(fresh);
        return ResponseEntity.created(URI.create("/api/owners/"+saved.getId())).body(saved);
    }
    @PutMapping("/{id}") @Transactional public Owner update(@PathVariable Integer id, @Valid @RequestBody Owner input) {
        Owner owner=get(id); owner.updateFrom(input); return owners.save(owner);
    }
    /** No deletion, even for currently childless owners: no distributed race or callback. */
    @DeleteMapping("/{id}") public void delete(@PathVariable Integer id) {
        get(id);
        throw new ResponseStatusException(HttpStatus.CONFLICT,"Owner deletion is disabled to preserve legacy references");
    }
}
