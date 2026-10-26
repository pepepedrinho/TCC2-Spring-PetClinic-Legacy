package br.edu.tcc.ownerspets.api;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OwnersPetsController {

    private final OwnersPetsQueryService service;
    private final OwnerCommandService commands;
    private final PetCommandService petCommands;

    public OwnersPetsController(OwnersPetsQueryService service, OwnerCommandService commands, PetCommandService petCommands) {
        this.service = service;
        this.commands = commands;
        this.petCommands = petCommands;
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

    @PostMapping("/owners")
    public ResponseEntity<Void> createOwner(
            @jakarta.validation.Valid @RequestBody OwnerRequest request) {
        Integer id = commands.create(request);
        return ResponseEntity.created(
                java.net.URI.create("/api/owners/" + id)).build();
    }

    @PutMapping("/owners/{id}")
    public ResponseEntity<Void> updateOwner(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody OwnerRequest request) {
        commands.update(id, request);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/owners/{ownerId}/pets")
    public ResponseEntity<Void> createPet(
            @PathVariable Integer ownerId,
            @jakarta.validation.Valid @RequestBody PetRequest request) {
        Integer id = petCommands.create(ownerId, request);
        return ResponseEntity.created(
                java.net.URI.create("/api/pets/" + id)).build();
    }

    @PutMapping("/pets/{id}")
    public ResponseEntity<Void> updatePet(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody PetRequest request) {
        petCommands.update(id, request);
        return ResponseEntity.noContent().build();
    }}