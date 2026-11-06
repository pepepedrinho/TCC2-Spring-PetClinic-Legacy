package br.edu.tcc.owners;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

// Entidade do serviço criada pela extração: reúne os dados antes distribuídos entre Owner, Person e
// BaseEntity; não contém Pets. Comparação integral em docs/v2/estudo-codigo-antes-depois.md.
@Entity
@Table(name = "owners")
public class Owner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Size(max = 30)
    @Column(name = "first_name")
    private String firstName;

    @NotBlank
    @Size(max = 30)
    @Column(name = "last_name")
    private String lastName;

    @NotBlank
    @Size(max = 255)
    @Column(name = "address")
    private String address;

    @NotBlank
    @Size(max = 80)
    @Column(name = "city")
    private String city;

    @NotBlank
    @Size(max = 10)
    @Pattern(regexp = "[0-9]{1,10}")
    @Column(name = "telephone")
    private String telephone;

    public Integer getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String value) {
        this.firstName = value;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String value) {
        this.lastName = value;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String value) {
        this.address = value;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String value) {
        this.city = value;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String value) {
        this.telephone = value;
    }

    // Método novo do serviço, sem equivalente direto em 1.5.x: copia os campos editáveis.
    public void updateFrom(Owner source) {
        firstName = source.firstName;
        lastName = source.lastName;
        address = source.address;
        city = source.city;
        telephone = source.telephone;
    }
}
