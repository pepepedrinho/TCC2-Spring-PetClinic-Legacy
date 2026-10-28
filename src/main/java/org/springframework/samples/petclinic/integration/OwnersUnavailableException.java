package org.springframework.samples.petclinic.integration;
public class OwnersUnavailableException extends RuntimeException {
    public OwnersUnavailableException(Throwable cause) { super("Owners service unavailable", cause); }
}
