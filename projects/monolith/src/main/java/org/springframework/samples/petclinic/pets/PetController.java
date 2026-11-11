/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.pets;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.integration.IntegrationErrorHandler;
import org.springframework.samples.petclinic.integration.OwnersServiceHttpClient;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Controller
class PetController {

    private static final String VIEWS_PETS_CREATE_OR_UPDATE_FORM = "pets/createOrUpdatePetForm";
    private final PetRepository petRepository;
    private final OwnersServiceHttpClient ownersServiceHttpClient;

    @Autowired
    public PetController(PetRepository petRepository, OwnersServiceHttpClient ownersServiceHttpClient) {
        this.petRepository = petRepository;
        this.ownersServiceHttpClient = ownersServiceHttpClient;
    }

    @ModelAttribute
    public void populatePetForm(@PathVariable("ownerId") int ownerId, ModelMap model) {
        model.addAttribute("types", this.petRepository.findPetTypes());
        model.addAttribute("owner", this.ownersServiceHttpClient.findById(ownerId));
    }

    @InitBinder("owner")
    public void initOwnerBinder(WebDataBinder webDataBinder) {
        webDataBinder.setDisallowedFields("id");
    }

    @InitBinder("pet")
    public void initPetBinder(WebDataBinder webDataBinder) {
        webDataBinder.setValidator(new PetValidator());
        webDataBinder.setAllowedFields("name", "birthDate", "type");
    }

    @GetMapping("/owners/{ownerId}/pets/new")
    public String initCreationForm(Owner owner, ModelMap modelMap) {
        Pet pet = new Pet();
        owner.addPet(pet);
        modelMap.put("pet", pet);
        return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
    }

    @PostMapping("/owners/{ownerId}/pets/new")
    public String processCreationForm(Owner owner, @Valid Pet pet, BindingResult bindingResult, ModelMap modelMap) {
        this.petRepository.findByOwnerId(owner.getId()).forEach(owner::addPet);
        if (StringUtils.hasLength(pet.getName()) && pet.isNew() && owner.getPet(pet.getName(), true) != null) {
            bindingResult.rejectValue("name", "duplicate", "already exists");
        }
        owner.addPet(pet);
        if (bindingResult.hasErrors()) {
            modelMap.put("pet", pet);
            return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
        } else {
            this.petRepository.save(pet);
            return "redirect:/owners/{ownerId}";
        }
    }

    @GetMapping("/owners/{ownerId}/pets/{petId}/edit")
    public String initUpdateForm(@PathVariable("ownerId") int ownerId,
                                 @PathVariable("petId") int petId,
                                 Owner owner, ModelMap modelMap) {

        Pet pet = this.petRepository.findById(petId);

        // Valida se o Pet local pertence ao Owner informado na URL.
        checkOwnership(pet, ownerId);

        // Recompõe a associação com o Owner recuperado do microsserviço.
        pet.setOwner(owner);

        modelMap.put("pet", pet);
        return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
    }

    @PostMapping("/owners/{ownerId}/pets/{petId}/edit")
    public String processUpdateForm(@PathVariable("petId") int petId,
                                    @Valid Pet pet, BindingResult bindingResult,
                                    Owner owner, ModelMap modelMap) {

        // Verifica se o Pet local pertence ao Owner informado.
        checkOwnership(this.petRepository.findById(petId), owner.getId());

        // Mantém o ID do Pet correspondente à rota de edição.
        pet.setId(petId);

        if (bindingResult.hasErrors()) {
            pet.setOwner(owner);
            modelMap.put("pet", pet);
            return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
        } else {
            owner.addPet(pet);
            this.petRepository.save(pet);
            return "redirect:/owners/{ownerId}";
        }
    }

    // Valida o vínculo entre o Pet local e o Owner remoto,
    // impedindo operações com um Owner diferente do associado.
    private void checkOwnership(Pet pet, Integer ownerId) {
        if (pet == null || !ownerId.equals(pet.getOwnerId())) {
            throw IntegrationErrorHandler.notFound();
        }
    }
}
