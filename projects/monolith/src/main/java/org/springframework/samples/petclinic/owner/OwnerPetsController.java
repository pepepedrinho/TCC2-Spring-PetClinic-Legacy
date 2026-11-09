package org.springframework.samples.petclinic.owner;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Read model for Owner pages. All Pet and Visit writes stay in the legacy MVC controllers. */
@RestController
class OwnerPetsController {

    private final PetRepository petRepository;

    OwnerPetsController(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    // Reject forwarded requests even when servlet URL normalization changes the public path.
    @PostMapping(value = "/internal/owner-pets", headers = "!X-PetClinic-Gateway")
    Map<Integer, List<PetSummary>> findPets(@RequestBody List<Integer> ownerIds) {
        Map<Integer, List<PetSummary>> petsByOwner = new LinkedHashMap<>();
        ownerIds.forEach(id -> petsByOwner.put(id, new ArrayList<>()));
        if (!petsByOwner.isEmpty()) {
            petRepository.findWithVisitsByOwnerIds(petsByOwner.keySet()).stream()
                    .sorted(Comparator.comparing(Pet::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                            .thenComparing(Pet::getId))
                    .forEach(pet -> petsByOwner.get(pet.getOwnerId()).add(new PetSummary(pet)));
        }
        return petsByOwner;
    }

    private static String formatDate(Date date) {
        return date == null ? null : Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }

    static final class PetSummary {
        public final Integer id;
        public final String name;
        public final String birthDate;
        public final String type;
        public final List<VisitSummary> visits;

        PetSummary(Pet pet) {
            this.id = pet.getId();
            this.name = pet.getName();
            this.birthDate = formatDate(pet.getBirthDate());
            this.type = pet.getType().getName();
            this.visits = pet.getVisits().stream().map(VisitSummary::new).collect(Collectors.toList());
        }
    }

    static final class VisitSummary {
        public final String date;
        public final String description;

        VisitSummary(Visit visit) {
            this.date = formatDate(visit.getDate());
            this.description = visit.getDescription();
        }
    }
}
