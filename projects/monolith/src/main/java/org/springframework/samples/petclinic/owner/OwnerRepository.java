package org.springframework.samples.petclinic.owner;

/** Owner lookup needed by the legacy Pet and Visit forms. */
public interface OwnerRepository {

    Owner findById(Integer id);
}
