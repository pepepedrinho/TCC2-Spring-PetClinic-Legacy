package org.springframework.samples.petclinic.service;

import java.util.Collection;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetRepository;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.samples.petclinic.visit.VisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test of the Service and the Repository layer.
 * <p>
 * ClinicServiceSpringDataJpaTests subclasses benefit from the following services provided by the Spring
 * TestContext Framework: </p> <ul> <li><strong>Spring IoC container caching</strong> which spares us unnecessary set up
 * time between test execution.</li> <li><strong>Dependency Injection</strong> of test fixture instances, meaning that
 * we don't need to perform application context lookups. See the use of {@link Autowired @Autowired} on the <code>{@link
 * ClinicServiceTests#clinicService clinicService}</code> instance variable, which uses autowiring <em>by
 * type</em>. <li><strong>Transaction management</strong>, meaning each test method is executed in its own transaction,
 * which is automatically rolled back by default. Thus, even if tests insert or otherwise change database state, there
 * is no need for a teardown or cleanup script. <li> An {@link org.springframework.context.ApplicationContext
 * ApplicationContext} is also inherited and can be used for explicit bean lookup if necessary. </li> </ul>
 *
 * @author Ken Krebs
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 * @author Dave Syer
 */

@RunWith(SpringRunner.class)
@DataJpaTest(includeFilters = @ComponentScan.Filter(Service.class))
public class ClinicServiceTests {

    @Autowired
    protected OwnerRepository ownerRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.samples.petclinic.integration.OwnersClient ownersClient;

    @Autowired
    protected PetRepository petRepository;

    @Autowired
    protected VisitRepository visitRepository;

    @Autowired
    protected VetRepository vetRepository;

    @org.junit.Before
    public void remoteOwners() {
        org.mockito.Mockito.when(ownersClient.findById(org.mockito.Matchers.anyInt())).thenAnswer(call -> {
            Owner owner = new Owner();
            owner.setId((Integer) call.getArguments()[0]);
            owner.setFirstName(owner.getId() == 6 ? "Jean" : "George");
            owner.setLastName("Franklin");
            return owner;
        });
        org.mockito.Mockito.when(ownersClient.save(org.mockito.Matchers.any(Owner.class)))
                .thenAnswer(call -> call.getArguments()[0]);
    }

    @Test
    public void shouldFindSingleOwnerWithPet() {
        Owner owner = this.ownerRepository.findById(1);
        assertThat(owner.getLastName()).startsWith("Franklin");
        assertThat(owner.getPets().size()).isEqualTo(1);
        assertThat(owner.getPets().get(0).getType()).isNotNull();
        assertThat(owner.getPets().get(0).getType().getName()).isEqualTo("cat");
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java: shouldFindPetWithCorrectId
    // ============================================================
    // @Test
    //     public void shouldFindPetWithCorrectId() {
    //         Pet pet7 = this.pets.findById(7);
    //         assertThat(pet7.getName()).startsWith("Samantha");
    //         assertThat(pet7.getOwner().getFirstName()).isEqualTo("Jean");
    //
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // A asserção da V2 verifica ownerId local em vez de navegar em associação JPA para Owner.
    // ============================================================

    @Test
    public void shouldFindPetWithCorrectId() {
        Pet pet7 = this.petRepository.findById(7);
        assertThat(pet7.getName()).startsWith("Samantha");
        assertThat(pet7.getOwnerId()).isEqualTo(6);

    }

    @Test
    public void shouldFindAllPetTypes() {
        Collection<PetType> petTypes = this.petRepository.findPetTypes();

        PetType petType1 = EntityUtils.getById(petTypes, PetType.class, 1);
        assertThat(petType1.getName()).isEqualTo("cat");
        PetType petType4 = EntityUtils.getById(petTypes, PetType.class, 4);
        assertThat(petType4.getName()).isEqualTo("snake");
    }

    @Test
    @Transactional
    public void shouldInsertPetIntoDatabaseAndGenerateId() {
        Owner owner6 = this.ownerRepository.findById(6);
        int found = owner6.getPets().size();

        Pet pet = new Pet();
        pet.setName("bowser");
        Collection<PetType> types = this.petRepository.findPetTypes();
        pet.setType(EntityUtils.getById(types, PetType.class, 2));
        pet.setBirthDate(new Date());
        owner6.addPet(pet);
        assertThat(owner6.getPets().size()).isEqualTo(found + 1);

        this.petRepository.save(pet);
        this.ownerRepository.save(owner6);

        owner6 = this.ownerRepository.findById(6);
        assertThat(owner6.getPets().size()).isEqualTo(found + 1);
        // checks that id has been generated
        assertThat(pet.getId()).isNotNull();
    }

    @Test
    @Transactional
    public void shouldUpdatePetName() throws Exception {
        Pet pet7 = this.petRepository.findById(7);
        String oldName = pet7.getName();

        String newName = oldName + "X";
        pet7.setName(newName);
        this.petRepository.save(pet7);

        pet7 = this.petRepository.findById(7);
        assertThat(pet7.getName()).isEqualTo(newName);
    }

    @Test
    public void shouldFindVets() {
        Collection<Vet> vets = this.vetRepository.findAll();

        Vet vet = EntityUtils.getById(vets, Vet.class, 3);
        assertThat(vet.getLastName()).isEqualTo("Douglas");
        assertThat(vet.getNrOfSpecialties()).isEqualTo(2);
        assertThat(vet.getSpecialties().get(0).getName()).isEqualTo("dentistry");
        assertThat(vet.getSpecialties().get(1).getName()).isEqualTo("surgery");
    }

    @Test
    @Transactional
    public void shouldAddNewVisitForPet() {
        Pet pet7 = this.petRepository.findById(7);
        int found = pet7.getVisits().size();
        Visit visit = new Visit();
        pet7.addVisit(visit);
        visit.setDescription("test");
        this.visitRepository.save(visit);
        this.petRepository.save(pet7);

        pet7 = this.petRepository.findById(7);
        assertThat(pet7.getVisits().size()).isEqualTo(found + 1);
        assertThat(visit.getId()).isNotNull();
    }

    @Test
    public void shouldFindVisitsByPetId() throws Exception {
        Collection<Visit> visits = this.visitRepository.findByPetId(7);
        assertThat(visits.size()).isEqualTo(2);
        Visit[] visitArr = visits.toArray(new Visit[visits.size()]);
        assertThat(visitArr[0].getDate()).isNotNull();
        assertThat(visitArr[0].getPetId()).isEqualTo(7);
    }

}
