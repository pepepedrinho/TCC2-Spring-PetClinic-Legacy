package org.springframework.samples.petclinic.integration;
@org.springframework.web.bind.annotation.ResponseStatus(value = org.springframework.http.HttpStatus.NOT_FOUND, reason = "Owner or Pet not found")
public class OwnerNotFoundException extends RuntimeException { }
