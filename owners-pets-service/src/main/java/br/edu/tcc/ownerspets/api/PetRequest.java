package br.edu.tcc.ownerspets.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PetRequest(
        @NotBlank String name,
        @NotNull LocalDate birthDate,
        @NotNull Integer typeId
) {}