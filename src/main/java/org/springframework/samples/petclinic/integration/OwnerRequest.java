package org.springframework.samples.petclinic.integration;

import org.springframework.samples.petclinic.owner.Owner;

public class OwnerRequest {

    private String firstName;
    private String lastName;
    private String address;
    private String city;
    private String telephone;

    public OwnerRequest(Owner owner) {
        this.firstName = owner.getFirstName();
        this.lastName = owner.getLastName();
        this.address = owner.getAddress();
        this.city = owner.getCity();
        this.telephone = owner.getTelephone();
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getTelephone() { return telephone; }
}