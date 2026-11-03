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
package org.springframework.samples.petclinic.owner;

import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.samples.petclinic.visit.VisitRepository;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 */
@Controller
class VisitController {

    private final VisitRepository visitRepository;

    private final PetRepository petRepository;

    private final OwnerRepository ownerRepository;

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/owner/VisitController.java: VisitController
    // ============================================================
    // @Autowired
    //     public VisitController(VisitRepository visits, PetRepository pets) {
    //         this.visits = visits;
    //         this.pets = pets;
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O fluxo MVC recompõe o Owner remoto e valida o vínculo do Pet antes de criar a visita.
    // ============================================================

    @Autowired
    public VisitController(VisitRepository visitRepository, PetRepository petRepository,
            OwnerRepository ownerRepository) {
        this.visitRepository = visitRepository;
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/owner/VisitController.java: setAllowedFields
    // ============================================================
    // @InitBinder
    //     public void setAllowedFields(WebDataBinder dataBinder) {
    //         dataBinder.setDisallowedFields("id");
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O fluxo MVC recompõe o Owner remoto e valida o vínculo do Pet antes de criar a visita.
    // ============================================================

    @InitBinder
    public void setAllowedFields(WebDataBinder webDataBinder) {
        webDataBinder.setAllowedFields("date", "description");
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/owner/VisitController.java: loadPetWithVisit
    // ============================================================
    // /**
    //      * Called before each and every @RequestMapping annotated method.
    //      * 2 goals:
    //      * - Make sure we always have fresh data
    //      * - Since we do not use the session scope, make sure that Pet object always has an id
    //      * (Even though id is not part of the form fields)
    //      *
    //      * @param petId
    //      * @return Pet
    //      */
    //     @ModelAttribute("visit")
    //     public Visit loadPetWithVisit(@PathVariable("petId") int petId, Map<String, Object> model) {
    //         Pet pet = this.pets.findById(petId);
    //         model.put("pet", pet);
    //         Visit visit = new Visit();
    //         pet.addVisit(visit);
    //         return visit;
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O fluxo MVC recompõe o Owner remoto e valida o vínculo do Pet antes de criar a visita.
    // ============================================================

    /**
     * Called before each and every @RequestMapping annotated method.
     * 2 goals:
     * - Make sure we always have fresh data
     * - Since we do not use the session scope, make sure that Pet object always has an id
     * (Even though id is not part of the form fields)
     *
     * @param petId
     * @return Pet
     */
    @ModelAttribute("visit")
    public Visit loadPetWithVisit(@PathVariable("ownerId") int ownerId, @PathVariable("petId") int petId,
            Map<String, Object> model) {
        Pet pet = this.petRepository.findById(petId);
        if (pet == null || !Integer.valueOf(ownerId).equals(pet.getOwnerId())) {
            // Antes havia uma exceção exclusiva; o handler central agora define o erro de vínculo.
            throw org.springframework.samples.petclinic.integration.IntegrationErrorHandler.notFound();
        }
        pet.setOwner(this.ownerRepository.findById(ownerId));
        model.put("pet", pet);
        Visit visit = new Visit();
        pet.addVisit(visit);
        return visit;
    }

    // Spring MVC calls method loadPetWithVisit(...) before initNewVisitForm is called
    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/owner/VisitController.java: initNewVisitForm
    // ============================================================
    // @GetMapping("/owners/*/pets/{petId}/visits/new")
    //     public String initNewVisitForm(@PathVariable("petId") int petId, Map<String, Object> model) {
    //         return "pets/createOrUpdateVisitForm";
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O fluxo MVC recompõe o Owner remoto e valida o vínculo do Pet antes de criar a visita.
    // ============================================================

    @GetMapping("/owners/{ownerId}/pets/{petId}/visits/new")
    public String initNewVisitForm(@PathVariable("petId") int petId, Map<String, Object> model) {
        return "pets/createOrUpdateVisitForm";
    }

    // Spring MVC calls method loadPetWithVisit(...) before processNewVisitForm is called
    @PostMapping("/owners/{ownerId}/pets/{petId}/visits/new")
    public String processNewVisitForm(@Valid Visit visit, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "pets/createOrUpdateVisitForm";
        } else {
            this.visitRepository.save(visit);
            return "redirect:/owners/{ownerId}";
        }
    }

}
