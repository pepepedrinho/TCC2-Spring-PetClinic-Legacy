package br.edu.tcc.owners;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
@Entity
@Table(name="owners")
public class Owner {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;
    public Integer getId() { return id; }
    @NotBlank @Size(max=30)
    @Column(name="first_name")
    private String firstName;
    public String getFirstName() { return firstName; }
    public void setFirstName(String value) { this.firstName = value; }
    @NotBlank @Size(max=30)
    @Column(name="last_name")
    private String lastName;
    public String getLastName() { return lastName; }
    public void setLastName(String value) { this.lastName = value; }
    @NotBlank @Size(max=255)
    @Column(name="address")
    private String address;
    public String getAddress() { return address; }
    public void setAddress(String value) { this.address = value; }
    @NotBlank @Size(max=80)
    @Column(name="city")
    private String city;
    public String getCity() { return city; }
    public void setCity(String value) { this.city = value; }
    @NotBlank @Size(max=10)
    @Pattern(regexp="[0-9]{1,10}")
    @Column(name="telephone")
    private String telephone;
    public String getTelephone() { return telephone; }
    public void setTelephone(String value) { this.telephone = value; }
    public void updateFrom(Owner source) {
        firstName=source.firstName; lastName=source.lastName; address=source.address;
        city=source.city; telephone=source.telephone;
    }
}
