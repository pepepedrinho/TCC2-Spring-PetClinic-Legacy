package br.edu.tcc.owners;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import br.edu.tcc.integration.LegacyPetsHttpClient;

@Controller
public class OwnerController {

    private static final String FORM = "owners/createOrUpdateOwnerForm";
    private static final String SEARCH = "owners/findOwners";

    private final OwnerRepository ownerRepository;
    private final LegacyPetsHttpClient legacyPetsHttpClient;

    public OwnerController(OwnerRepository ownerRepository, LegacyPetsHttpClient legacyPetsHttpClient) {
        this.ownerRepository = ownerRepository;
        this.legacyPetsHttpClient = legacyPetsHttpClient;
    }

    @GetMapping("/api/owners")
    @ResponseBody
    public List<Owner> search(@RequestParam(defaultValue = "") String lastName) {
        return ownerRepository.findByLastName(lastName);
    }

    @GetMapping("/api/owners/{id}")
    @ResponseBody
    public Owner get(@PathVariable Integer id) {
        return findOwner(id);
    }

    @PostMapping("/api/owners")
    public ResponseEntity<Owner> create(@Valid @RequestBody Owner owner) {
        Owner newOwner = new Owner();
        newOwner.updateFrom(owner);
        Owner savedOwner = ownerRepository.save(newOwner);
        return ResponseEntity.created(URI.create("/api/owners/" + savedOwner.getId())).body(savedOwner);
    }

    @PutMapping("/api/owners/{id}")
    @ResponseBody
    @Transactional
    public Owner update(@PathVariable Integer id, @Valid @RequestBody Owner input) {
        Owner owner = findOwner(id);
        owner.updateFrom(input);
        return ownerRepository.save(owner);
    }

    /** No deletion, even for currently childless owners: no distributed race or callback. */
    @DeleteMapping("/api/owners/{id}")
    public void delete(@PathVariable Integer id) {
        findOwner(id);
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Owner deletion is disabled to preserve legacy references");
    }

    @InitBinder("owner")
    void bindOwner(WebDataBinder binder) {
        binder.setAllowedFields("firstName", "lastName", "address", "city", "telephone");
    }

    @GetMapping("/owners/find")
    String findForm(@ModelAttribute("owner") Owner owner) {
        return SEARCH;
    }

    @GetMapping("/owners")
    String search(@ModelAttribute("owner") Owner owner, BindingResult errors, Model model) {
        String lastName = owner.getLastName() == null ? "" : owner.getLastName();
        List<Owner> matches = ownerRepository.findByLastName(lastName);
        if (matches.isEmpty()) {
            errors.rejectValue("lastName", "notFound", "not found");
            return SEARCH;
        }
        if (matches.size() == 1) {
            return "redirect:/owners/" + matches.get(0).getId();
        }

        model.addAttribute("selections", matches);
        model.addAttribute("petsByOwner", legacyPetsHttpClient.findPets(matches.stream().map(Owner::getId).toList()));
        return "owners/ownersList";
    }

    @GetMapping("/owners/new")
    String createForm(Model model) {
        model.addAttribute("owner", new Owner());
        return FORM;
    }

    @PostMapping("/owners/new")
    String create(@Valid @ModelAttribute("owner") Owner owner, BindingResult errors) {
        if (errors.hasErrors()) {
            return FORM;
        }
        return "redirect:/owners/" + ownerRepository.save(owner).getId();
    }

    @GetMapping("/owners/{ownerId}")
    String details(@PathVariable Integer ownerId, Model model) {
        model.addAttribute("owner", findOwner(ownerId));
        model.addAttribute("pets", legacyPetsHttpClient.findPets(List.of(ownerId)).get(ownerId));
        return "owners/ownerDetails";
    }

    @GetMapping("/owners/{ownerId}/edit")
    String editForm(@PathVariable Integer ownerId, Model model) {
        model.addAttribute("owner", findOwner(ownerId));
        model.addAttribute("editing", true);
        return FORM;
    }

    @PostMapping("/owners/{ownerId}/edit")
    @Transactional
    String update(@PathVariable Integer ownerId, @Valid @ModelAttribute("owner") Owner input,
            BindingResult errors, Model model) {
        Owner owner = findOwner(ownerId);
        if (errors.hasErrors()) {
            model.addAttribute("editing", true);
            return FORM;
        }
        owner.updateFrom(input);
        return "redirect:/owners/{ownerId}";
    }

    private Owner findOwner(Integer ownerId) {
        return ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
