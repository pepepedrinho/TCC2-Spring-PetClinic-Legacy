package org.springframework.samples.petclinic.integration;
@org.springframework.web.bind.annotation.ResponseStatus(value = org.springframework.http.HttpStatus.BAD_REQUEST, reason = "Invalid owner data")
public class OwnerValidationException extends RuntimeException { }
