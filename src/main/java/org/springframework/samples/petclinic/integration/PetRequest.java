package org.springframework.samples.petclinic.integration;

import java.time.LocalDate;
import org.springframework.samples.petclinic.owner.Pet;

public class PetRequest {

    private final String name;
    private final String birthDate;
    private final Integer typeId;

    public PetRequest(Pet pet) {
        this.name = pet.getName();
        this.birthDate = pet.getBirthDate() == null ? null :
                new java.sql.Date(pet.getBirthDate().getTime())
                        .toLocalDate().toString();
        this.typeId = pet.getType() == null ? null : pet.getType().getId();
    }

    public String getName() {
        return name;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public Integer getTypeId() {
        return typeId;
    }
}