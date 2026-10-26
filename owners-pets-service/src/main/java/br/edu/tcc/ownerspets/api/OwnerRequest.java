package br.edu.tcc.ownerspets.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;

public record OwnerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank @Digits(integer = 10, fraction = 0) String telephone
) {}