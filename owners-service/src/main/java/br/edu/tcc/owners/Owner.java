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

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/model/BaseEntity.java: getId
    // ============================================================
    // public Integer getId() {
    //         return id;
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O serviço acessa seu próprio estado persistente; a API e as validações já eram as da V2.
    // ============================================================

    public Integer getId() {
        return id;
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/model/Person.java: getFirstName
    // ============================================================
    // public String getFirstName() {
    //         return this.firstName;
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O serviço acessa seu próprio estado persistente; a API e as validações já eram as da V2.
    // ============================================================

    public String getFirstName() {
        return firstName;
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/model/Person.java: setFirstName
    // ============================================================
    // public void setFirstName(String firstName) {
    //         this.firstName = firstName;
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O serviço acessa seu próprio estado persistente; a API e as validações já eram as da V2.
    // ============================================================

    public void setFirstName(String value) {
        this.firstName = value;
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/model/Person.java: getLastName
    // ============================================================
    // public String getLastName() {
    //         return this.lastName;
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O serviço acessa seu próprio estado persistente; a API e as validações já eram as da V2.
    // ============================================================

    public String getLastName() {
        return lastName;
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/model/Person.java: setLastName
    // ============================================================
    // public void setLastName(String lastName) {
    //         this.lastName = lastName;
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O serviço acessa seu próprio estado persistente; a API e as validações já eram as da V2.
    // ============================================================

    public void setLastName(String value) {
        this.lastName = value;
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/owner/Owner.java: getAddress
    // ============================================================
    // public String getAddress() {
    //         return this.address;
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O serviço acessa seu próprio estado persistente; a API e as validações já eram as da V2.
    // ============================================================

    public String getAddress() {
        return address;
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/owner/Owner.java: setAddress
    // ============================================================
    // public void setAddress(String address) {
    //         this.address = address;
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O serviço acessa seu próprio estado persistente; a API e as validações já eram as da V2.
    // ============================================================

    public void setAddress(String value) {
        this.address = value;
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/owner/Owner.java: getCity
    // ============================================================
    // public String getCity() {
    //         return this.city;
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O serviço acessa seu próprio estado persistente; a API e as validações já eram as da V2.
    // ============================================================

    public String getCity() {
        return city;
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/owner/Owner.java: setCity
    // ============================================================
    // public void setCity(String city) {
    //         this.city = city;
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O serviço acessa seu próprio estado persistente; a API e as validações já eram as da V2.
    // ============================================================

    public void setCity(String value) {
        this.city = value;
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/owner/Owner.java: getTelephone
    // ============================================================
    // public String getTelephone() {
    //         return this.telephone;
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O serviço acessa seu próprio estado persistente; a API e as validações já eram as da V2.
    // ============================================================

    public String getTelephone() {
        return telephone;
    }

    // ============================================================
    // IMPLEMENTAÇÃO ORIGINAL — MONÓLITO (branch 1.5.x)
    // Origem: src/main/java/org/springframework/samples/petclinic/owner/Owner.java: setTelephone
    // ============================================================
    // public void setTelephone(String telephone) {
    //         this.telephone = telephone;
    //     }
    // ============================================================
    // IMPLEMENTAÇÃO ATUAL — STRANGLER FIG V2
    // O serviço acessa seu próprio estado persistente; a API e as validações já eram as da V2.
    // ============================================================

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
