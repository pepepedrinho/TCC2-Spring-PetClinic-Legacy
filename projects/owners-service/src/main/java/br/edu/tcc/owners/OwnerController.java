package br.edu.tcc.owners;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import br.edu.tcc.integration.LegacyPetsHttpClient;

@Controller
public class OwnerController {

    private static final String FORM = "owners/createOrUpdateOwnerForm";
    private static final String SEARCH = "owners/findOwners";

    private final OwnerRepository ownerRepository;
    private final OwnerLookup ownerLookup;
    private final LegacyPetsHttpClient legacyPetsHttpClient;

    public OwnerController(OwnerRepository ownerRepository, OwnerLookup ownerLookup,
            LegacyPetsHttpClient legacyPetsHttpClient) {
        this.ownerRepository = ownerRepository;
        this.ownerLookup = ownerLookup;
        this.legacyPetsHttpClient = legacyPetsHttpClient;
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
        model.addAttribute("owner", ownerLookup.findOwner(ownerId));
        model.addAttribute("pets", legacyPetsHttpClient.findPets(List.of(ownerId)).get(ownerId));
        return "owners/ownerDetails";
    }

    @GetMapping("/owners/{ownerId}/edit")
    String editForm(@PathVariable Integer ownerId, Model model) {
        model.addAttribute("owner", ownerLookup.findOwner(ownerId));
        model.addAttribute("editing", true);
        return FORM;
    }

    @PostMapping("/owners/{ownerId}/edit")
    @Transactional
    String update(@PathVariable Integer ownerId, @Valid @ModelAttribute("owner") Owner input,
            BindingResult errors, Model model) {
        Owner owner = ownerLookup.findOwner(ownerId);
        if (errors.hasErrors()) {
            model.addAttribute("editing", true);
            return FORM;
        }
        owner.updateFrom(input);
        return "redirect:/owners/{ownerId}";
    }
}
