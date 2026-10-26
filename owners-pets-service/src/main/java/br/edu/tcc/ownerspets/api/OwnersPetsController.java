package br.edu.tcc.ownerspets.api;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OwnersPetsController {

    private final OwnersPetsQueryService service;

    public OwnersPetsController(OwnersPetsQueryService service) {
        this.service = service;
    }

    @GetMapping("/owners/{id}")
    public ResponseEntity<OwnersPetsQueryService.OwnerDto> owner(
            @PathVariable Integer id) {
        return ResponseEntity.of(
                java.util.Optional.ofNullable(service.findOwner(id)));
    }

    @GetMapping("/owners")
    public List<OwnersPetsQueryService.OwnerDto> search(
            @RequestParam(defaultValue = "") String lastName) {
        return service.searchOwners(lastName);
    }

    @GetMapping("/pets/{id}")
    public ResponseEntity<OwnersPetsQueryService.PetDto> pet(
            @PathVariable Integer id) {
        return ResponseEntity.of(
                java.util.Optional.ofNullable(service.findPet(id)));
    }

    @GetMapping("/pet-types")
    public List<OwnersPetsQueryService.PetTypeDto> petTypes() {
        return service.findTypes();
    }
}