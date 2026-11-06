package org.springframework.samples.petclinic.owner;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class OwnerTests {

    @Test
    public void addsNewAndPersistedPetsAndKeepsBothSidesInSync() {
        Owner owner = new Owner();
        owner.setId(6);
        Pet newPet = new Pet();
        Pet persistedPet = new Pet();
        persistedPet.setId(7);

        owner.addPet(newPet);
        owner.addPet(persistedPet);
        owner.addPet(persistedPet);
        owner.addPet(newPet);

        assertEquals(2, owner.getPetsInternal().size());
        for (Pet pet : new Pet[] {newPet, persistedPet}) {
            assertTrue(owner.getPetsInternal().contains(pet));
            assertSame(owner, pet.getOwner());
            assertEquals(owner.getId(), pet.getOwnerId());
        }
    }
}
