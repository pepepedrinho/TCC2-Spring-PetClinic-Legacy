# Estudo do código antes e depois — Strangler Fig V2

Este documento acompanha a organização e documentação do código em 03/11/2026. A implementação funcional da V2 já existia: esta intervenção muda apresentação, ordem de declarações e comentários. Não representa uma nova extração ou revisão dos contratos existentes.

> Atualização posterior: as seções 1–10 e os apêndices registram a V2 organizada antes da centralização (cenário B). A seção 11 descreve a intervenção posterior de tratamento de erros (cenário C). Os trechos históricos foram preservados como registro desses checkpoints.

## 1. Origem dos trechos e limites da comparação

Os trechos antigos foram recuperados por Git, exclusivamente de `refs/heads/1.5.x`, commit `c36452a2c34443ae26b4ecbba4f149906af14717`. A referência completa evita a ambiguidade entre branch e tag `1.5.x`.

```powershell
git status --short
git show-ref --verify refs/heads/1.5.x
git diff --name-status refs/heads/1.5.x -- '*.java'
git show refs/heads/1.5.x:src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java
```

A árvore de destino é a branch `tcc2-strangler-fig-v2`, baseada no HEAD `d3fea1a`, incluindo os quatro ajustes manuais de formatação encontrados em `owners-service`. Os trechos atuais abaixo foram extraídos dos fontes organizados. Os antigos mantêm sua sintaxe original; as linhas `//` nos fontes apenas os desativam para estudo.

Não se presume equivalência entre camadas diferentes. Uma declaração do Spring Data não contém implementação manual: a implementação é gerada pelo framework. O controller REST novo não corresponde diretamente ao controller MVC de formulários. Métodos e componentes novos são identificados como novos, sem um passado inventado.

## 2. Persistência original de Owner no monólito

`src/main/java/org/springframework/samples/petclinic/owner/Owner.java` era uma entidade `@Entity`, mapeada para `owners`, e herdava nomes de `model/Person.java` e identidade de `model/BaseEntity.java`. `address`, `city` e `telephone` eram colunas locais. A coleção `pets` era mapeada por `@OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")`.

`owner/OwnerRepository.java` era uma interface `Repository<Owner, Integer>`. `findByLastName` e `findById` utilizavam JPQL com `left join fetch owner.pets`; `save` era uma declaração implementada pelo Spring Data. Assim, o acesso a Owner e a composição dos seus Pets eram resolvidos no mesmo contexto JPA. A implementação original integral de Owner, Person, BaseEntity e OwnerRepository está no apêndice A.

## 3. Consulta pelo OwnersClient na V2

O nome `OwnerRepository` é preservado no legado para servir aos controllers existentes, mas a classe agora é um `@Service` adaptador. `findById` consulta `OwnersClient` e chama `compose`; `findByLastName` compõe cada resultado; `save` envia apenas dados de Owner e devolve o identificador à instância usada pelo fluxo MVC.

`src/main/java/org/springframework/samples/petclinic/integration/OwnersClient.java` é novo. Sua URL recebe `/api/owners`; a consulta por sobrenome codifica o parâmetro de URI. O payload de gravação contém somente `firstName`, `lastName`, `address`, `city` e `telephone`. Pets e Visits continuam no legado. O construtor preserva timeout de conexão de 1500 ms e de leitura de 3000 ms.

`translate` preserva a tradução: HTTP 404 → `OwnerNotFoundException`; HTTP 400 → `OwnerValidationException`; demais falhas capturadas, inclusive conversão de mensagem, → `OwnersUnavailableException`. `IntegrationErrorHandler.unavailable` mantém a página `ownersUnavailable` com HTTP 503.

## 4. Persistência no microsserviço

Em `owners-service/src/main/java/br/edu/tcc/owners/Owner.java`, identidade e dados pessoais estão reunidos em uma entidade independente, com anotações Jakarta. Ela não herda `Person` ou `BaseEntity` do monólito e não possui uma coleção de Pets. Mantêm-se exatamente as validações já existentes na V2, inclusive os limites de tamanho e o padrão numérico de telefone; não se afirma que as validações sejam idênticas às de 1.5.x.

`owners-service/.../OwnerRepository.java` estende `JpaRepository<Owner, Integer>`. `findById` e `save` são herdados do framework; não há corpo Java local equivalente às declarações antigas. A busca `findByLastNameStartingWithIgnoreCaseOrderByLastNameAscIdAsc` usa prefixo, ignora maiúsculas/minúsculas e ordena por sobrenome e id. Isso descreve o comportamento que já existia na V2, sem afirmar igualdade textual ou semântica com a JPQL de 1.5.x.

`OwnerController` do serviço expõe `GET /api/owners` e `GET /api/owners/{id}`, cria com `POST` e resposta 201 com `Location`, e atualiza com `PUT`. Um id ausente resulta em 404. `create` copia os campos para uma instância nova; `updateFrom` não copia id. `DELETE` de Owner existente resulta em 409 para proteger referências legadas; id inexistente continua em 404. Essas decisões já faziam parte da V2 e não foram alteradas pela organização.

## 5. Relação entre Owner e Pet

`src/main/java/org/springframework/samples/petclinic/owner/Pet.java` continua entidade JPA do legado. Na origem, `owner` tinha `@ManyToOne` e `@JoinColumn(name = "owner_id")`. Na V2, `ownerId` é o atributo persistente e `owner` é `@Transient`. `setOwner` atribui o objeto transitório e copia seu identificador para `ownerId`.

`PetRepository.findByOwnerId` é novo e consulta os Pets locais para `OwnerRepository.compose`. `Owner.addPet` passou de `pet.isNew()` para uma verificação de presença na coleção, permitindo compor Pets existentes. O Owner legado é um modelo de apresentação sem `@Entity`, preservando o uso nas telas. O relacionamento `Pet`–`Visit` permanece local; não há JPA distribuído nem transferência de negócio de Pets ou Visits ao serviço.

Na organização atual, os atributos persistentes de Pet ficam juntos, antes do Owner transitório. A ordem relativa dos inicializadores persistentes foi preservada; a movimentação do campo transitório sem inicializador não introduz dependências de inicialização.

## 6. Controllers do monólito

`src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java` permaneceu inalterado em relação a 1.5.x e nesta intervenção. Seus formulários, validações MVC e redirecionamentos continuam chamando o mesmo nome `OwnerRepository`; a fronteira HTTP está atrás desse adaptador. Por isso, não recebeu cópias históricas redundantes.

`PetController` mantém os fluxos MVC, com as mudanças que já existiam na V2: binding limitado a `name`, `birthDate` e `type`, validação de pertencimento do Pet pelo `ownerId`, recomposição do Owner e uso do id da rota na atualização. `checkOwnership` é um auxiliar novo da integração.

`VisitController` recebeu dependência do adaptador de Owner e recompõe o Owner remoto após validar o Pet local. O binding permite apenas `date` e `description`. A rota GET original usava `*` para Owner e a V2 explicita `{ownerId}`; os trechos completos estão nas comparações. A rota POST é preservada conforme a implementação V2. Não houve alteração de endpoints durante esta tarefa.

## 7. Responsabilidade do Gateway

`gateway/src/main/java/br/edu/tcc/gateway/GatewayApplication.java` é novo e apenas inicia o Spring Boot. As decisões de encaminhamento estão em `gateway/src/main/resources/application.properties`: porta 8081, rota `owners-service` com ordem -1 para `/api/owners` e `/api/owners/**` em `http://localhost:8082`, e rota `petclinic-monolith` com ordem 0 para `/**` em `http://localhost:8080`. Ambas preservam `PreserveHostHeader`.

O Gateway não persiste entidades nem extrai Pets ou Visits. O legado continua na porta 8080 e Owners na porta 8082; a configuração de roteamento e as propriedades não foram editadas.

## 8. Mapa de classes e responsabilidades

| Situação | Arquivos/classes | Responsabilidade |
| --- | --- | --- |
| Mantidos | OwnerController legado; Person; BaseEntity; entidades e repositórios de Vet/Visit e demais fontes iguais à origem | Fluxos MVC e domínio que permanecem locais; sem formatação fora do escopo V2 |
| Alterados no legado | Owner; OwnerRepository; Pet; PetRepository; PetController; VisitController | Modelo de apresentação, fronteira HTTP, composição e identificação do Owner remoto |
| Dados extraídos | owners-service/Owner | Reúne dados originais de Owner, Person e BaseEntity, sem Pets |
| Novos no serviço | OwnersApplication; OwnerRepository; OwnerController | Aplicação independente, JPA próprio e contrato REST; controllers sem equivalência direta com formulários antigos |
| Novos na integração | OwnersClient; IntegrationErrorHandler; OwnersUnavailableException; OwnerNotFoundException; OwnerValidationException | Comunicação HTTP, tradução de erros e resposta de indisponibilidade |
| Novos na coexistência | LegacyDatabaseInitializer; GatewayApplication; MigrateOwners | Inicialização de banco vazio, entrada HTTP e migração offline |
| Testes alterados | PetControllerTests; VisitControllerTests; ClinicServiceTests | Fixtures e verificações adaptadas ao Owner remoto |
| Testes novos | OwnerApiTests; GatewayRoutesTests; OwnersClientTests | Contrato REST, rotas e fronteira HTTP |

`LegacyDatabaseInitializer` não tem equivalente Java direto em 1.5.x: a inicialização anterior era infraestrutura/configuração do monólito. `MigrateOwners`, os testes de integração novos e as exceções também não têm equivalente direto. `Owner.updateFrom`, `OwnerRepository.compose`, `PetRepository.findByOwnerId`, `Pet.getOwnerId`, `PetController.checkOwnership` e a fixture `ClinicServiceTests.remoteOwners` são métodos novos; não receberam implementações antigas inventadas.

O acesso a id, nomes e contatos tem origem identificável nos getters/setters das três classes originais. Essa transferência de responsabilidade é comparada abaixo, mesmo quando o corpo do getter é semelhante; não significa identidade entre as entidades ou seus contratos completos.

## 9. Comparações de métodos com código real

Cada par identifica arquivo e método de origem e destino. As diferenças funcionais aqui descritas pertencem à extração já implementada na V2; esta tarefa apenas as documenta. Métodos originais inalterados não recebem comparações repetidas, exceto os acessores cuja responsabilidade foi transferida à entidade do serviço.

### 9.1. Owner.getId

Origem: `src/main/java/org/springframework/samples/petclinic/model/BaseEntity.java` → `getId` em `refs/heads/1.5.x`.

Destino: `owners-service/src/main/java/br/edu/tcc/owners/Owner.java` → `getId` na V2.

Responsabilidade de acesso ao dado transferida para a entidade independente do serviço.

Original:

```java
public Integer getId() {
        return id;
    }
```

V2:

```java
public Integer getId() {
        return id;
    }
```

### 9.2. Owner.getFirstName

Origem: `src/main/java/org/springframework/samples/petclinic/model/Person.java` → `getFirstName` em `refs/heads/1.5.x`.

Destino: `owners-service/src/main/java/br/edu/tcc/owners/Owner.java` → `getFirstName` na V2.

Responsabilidade de acesso ao dado transferida para a entidade independente do serviço.

Original:

```java
public String getFirstName() {
        return this.firstName;
    }
```

V2:

```java
public String getFirstName() {
        return firstName;
    }
```

### 9.3. Owner.setFirstName

Origem: `src/main/java/org/springframework/samples/petclinic/model/Person.java` → `setFirstName` em `refs/heads/1.5.x`.

Destino: `owners-service/src/main/java/br/edu/tcc/owners/Owner.java` → `setFirstName` na V2.

Responsabilidade de acesso ao dado transferida para a entidade independente do serviço.

Original:

```java
public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
```

V2:

```java
public void setFirstName(String value) {
        this.firstName = value;
    }
```

### 9.4. Owner.getLastName

Origem: `src/main/java/org/springframework/samples/petclinic/model/Person.java` → `getLastName` em `refs/heads/1.5.x`.

Destino: `owners-service/src/main/java/br/edu/tcc/owners/Owner.java` → `getLastName` na V2.

Responsabilidade de acesso ao dado transferida para a entidade independente do serviço.

Original:

```java
public String getLastName() {
        return this.lastName;
    }
```

V2:

```java
public String getLastName() {
        return lastName;
    }
```

### 9.5. Owner.setLastName

Origem: `src/main/java/org/springframework/samples/petclinic/model/Person.java` → `setLastName` em `refs/heads/1.5.x`.

Destino: `owners-service/src/main/java/br/edu/tcc/owners/Owner.java` → `setLastName` na V2.

Responsabilidade de acesso ao dado transferida para a entidade independente do serviço.

Original:

```java
public void setLastName(String lastName) {
        this.lastName = lastName;
    }
```

V2:

```java
public void setLastName(String value) {
        this.lastName = value;
    }
```

### 9.6. Owner.getAddress

Origem: `src/main/java/org/springframework/samples/petclinic/owner/Owner.java` → `getAddress` em `refs/heads/1.5.x`.

Destino: `owners-service/src/main/java/br/edu/tcc/owners/Owner.java` → `getAddress` na V2.

Responsabilidade de acesso ao dado transferida para a entidade independente do serviço.

Original:

```java
public String getAddress() {
        return this.address;
    }
```

V2:

```java
public String getAddress() {
        return address;
    }
```

### 9.7. Owner.setAddress

Origem: `src/main/java/org/springframework/samples/petclinic/owner/Owner.java` → `setAddress` em `refs/heads/1.5.x`.

Destino: `owners-service/src/main/java/br/edu/tcc/owners/Owner.java` → `setAddress` na V2.

Responsabilidade de acesso ao dado transferida para a entidade independente do serviço.

Original:

```java
public void setAddress(String address) {
        this.address = address;
    }
```

V2:

```java
public void setAddress(String value) {
        this.address = value;
    }
```

### 9.8. Owner.getCity

Origem: `src/main/java/org/springframework/samples/petclinic/owner/Owner.java` → `getCity` em `refs/heads/1.5.x`.

Destino: `owners-service/src/main/java/br/edu/tcc/owners/Owner.java` → `getCity` na V2.

Responsabilidade de acesso ao dado transferida para a entidade independente do serviço.

Original:

```java
public String getCity() {
        return this.city;
    }
```

V2:

```java
public String getCity() {
        return city;
    }
```

### 9.9. Owner.setCity

Origem: `src/main/java/org/springframework/samples/petclinic/owner/Owner.java` → `setCity` em `refs/heads/1.5.x`.

Destino: `owners-service/src/main/java/br/edu/tcc/owners/Owner.java` → `setCity` na V2.

Responsabilidade de acesso ao dado transferida para a entidade independente do serviço.

Original:

```java
public void setCity(String city) {
        this.city = city;
    }
```

V2:

```java
public void setCity(String value) {
        this.city = value;
    }
```

### 9.10. Owner.getTelephone

Origem: `src/main/java/org/springframework/samples/petclinic/owner/Owner.java` → `getTelephone` em `refs/heads/1.5.x`.

Destino: `owners-service/src/main/java/br/edu/tcc/owners/Owner.java` → `getTelephone` na V2.

Responsabilidade de acesso ao dado transferida para a entidade independente do serviço.

Original:

```java
public String getTelephone() {
        return this.telephone;
    }
```

V2:

```java
public String getTelephone() {
        return telephone;
    }
```

### 9.11. Owner.setTelephone

Origem: `src/main/java/org/springframework/samples/petclinic/owner/Owner.java` → `setTelephone` em `refs/heads/1.5.x`.

Destino: `owners-service/src/main/java/br/edu/tcc/owners/Owner.java` → `setTelephone` na V2.

Responsabilidade de acesso ao dado transferida para a entidade independente do serviço.

Original:

```java
public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
```

V2:

```java
public void setTelephone(String value) {
        this.telephone = value;
    }
```

### 9.12. Owner.addPet

Origem: `src/main/java/org/springframework/samples/petclinic/owner/Owner.java` → `addPet` em `refs/heads/1.5.x`.

Destino: `src/main/java/org/springframework/samples/petclinic/owner/Owner.java` → `addPet` na V2.

O Owner remoto é composto com Pets persistidos no legado.

Original:

```java
public void addPet(Pet pet) {
        if (pet.isNew()) {
            getPetsInternal().add(pet);
        }
        pet.setOwner(this);
    }
```

V2:

```java
public void addPet(Pet pet) {
        if (!getPetsInternal().contains(pet)) {
            getPetsInternal().add(pet);
        }
        pet.setOwner(this);
    }
```

### 9.13. OwnerRepository.findByLastName

Origem: `src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java` → `findByLastName` em `refs/heads/1.5.x`.

Destino: `src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java` → `findByLastName` na V2.

A persistência JPA local foi substituída por HTTP e composição com Pets locais.

Original:

```java
/**
     * Retrieve {@link Owner}s from the data store by last name, returning all owners
     * whose last name <i>starts</i> with the given name.
     * @param lastName Value to search for
     * @return a Collection of matching {@link Owner}s (or an empty Collection if none
     * found)
     */
    @Query("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets WHERE owner.lastName LIKE :lastName%")
    @Transactional(readOnly = true)
    Collection<Owner> findByLastName(@Param("lastName") String lastName);
```

V2:

```java
public Collection<Owner> findByLastName(String lastName) {
        Collection<Owner> owners = client.findByLastName(lastName);
        for (Owner owner : owners) {
            compose(owner);
        }
        return owners;
    }
```

### 9.14. OwnerRepository.findById

Origem: `src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java` → `findById` em `refs/heads/1.5.x`.

Destino: `src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java` → `findById` na V2.

A persistência JPA local foi substituída por HTTP e composição com Pets locais.

Original:

```java
/**
     * Retrieve an {@link Owner} from the data store by id.
     * @param id the id to search for
     * @return the {@link Owner} if found
     */
    @Query("SELECT owner FROM Owner owner left join fetch owner.pets WHERE owner.id =:id")
    @Transactional(readOnly = true)
    Owner findById(@Param("id") Integer id);
```

V2:

```java
public Owner findById(Integer id) {
        return compose(client.findById(id));
    }
```

### 9.15. OwnerRepository.save

Origem: `src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java` → `save` em `refs/heads/1.5.x`.

Destino: `src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java` → `save` na V2.

A persistência JPA local foi substituída por HTTP e composição com Pets locais.

Original:

```java
/**
     * Save an {@link Owner} to the data store, either inserting or updating it.
     * @param owner the {@link Owner} to save
     */
    void save(Owner owner);
```

V2:

```java
public void save(Owner owner) {
        owner.setId(client.save(owner).getId());
    }
```

### 9.16. Pet.setOwner

Origem: `src/main/java/org/springframework/samples/petclinic/owner/Pet.java` → `setOwner` em `refs/heads/1.5.x`.

Destino: `src/main/java/org/springframework/samples/petclinic/owner/Pet.java` → `setOwner` na V2.

O vínculo usa ownerId persistente e Owner transitório, composto pelo legado.

Original:

```java
protected void setOwner(Owner owner) {
        this.owner = owner;
    }
```

V2:

```java
protected void setOwner(Owner owner) {
        this.owner = owner;
        this.ownerId = owner.getId();
    }
```

### 9.17. PetController.initPetBinder

Origem: `src/main/java/org/springframework/samples/petclinic/owner/PetController.java` → `initPetBinder` em `refs/heads/1.5.x`.

Destino: `src/main/java/org/springframework/samples/petclinic/owner/PetController.java` → `initPetBinder` na V2.

O fluxo MVC da V2 restringe o binding e valida o vínculo local com o Owner remoto.

Original:

```java
@InitBinder("pet")
    public void initPetBinder(WebDataBinder dataBinder) {
        dataBinder.setValidator(new PetValidator());
    }
```

V2:

```java
@InitBinder("pet")
    public void initPetBinder(WebDataBinder dataBinder) {
        dataBinder.setValidator(new PetValidator());
        dataBinder.setAllowedFields("name", "birthDate", "type");
    }
```

### 9.18. PetController.initUpdateForm

Origem: `src/main/java/org/springframework/samples/petclinic/owner/PetController.java` → `initUpdateForm` em `refs/heads/1.5.x`.

Destino: `src/main/java/org/springframework/samples/petclinic/owner/PetController.java` → `initUpdateForm` na V2.

O fluxo MVC da V2 restringe o binding e valida o vínculo local com o Owner remoto.

Original:

```java
@GetMapping("/pets/{petId}/edit")
    public String initUpdateForm(@PathVariable("petId") int petId, ModelMap model) {
        Pet pet = this.pets.findById(petId);
        model.put("pet", pet);
        return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
    }
```

V2:

```java
@GetMapping("/pets/{petId}/edit")
    public String initUpdateForm(@PathVariable("ownerId") int ownerId, @PathVariable("petId") int petId, Owner owner,
            ModelMap model) {
        Pet pet = this.pets.findById(petId);
        checkOwnership(pet, ownerId);
        pet.setOwner(owner);
        model.put("pet", pet);
        return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
    }
```

### 9.19. PetController.processUpdateForm

Origem: `src/main/java/org/springframework/samples/petclinic/owner/PetController.java` → `processUpdateForm` em `refs/heads/1.5.x`.

Destino: `src/main/java/org/springframework/samples/petclinic/owner/PetController.java` → `processUpdateForm` na V2.

O fluxo MVC da V2 restringe o binding e valida o vínculo local com o Owner remoto.

Original:

```java
@PostMapping("/pets/{petId}/edit")
    public String processUpdateForm(@Valid Pet pet, BindingResult result, Owner owner, ModelMap model) {
        if (result.hasErrors()) {
            pet.setOwner(owner);
            model.put("pet", pet);
            return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
        } else {
            owner.addPet(pet);
            this.pets.save(pet);
            return "redirect:/owners/{ownerId}";
        }
    }
```

V2:

```java
@PostMapping("/pets/{petId}/edit")
    public String processUpdateForm(@PathVariable("petId") int petId, @Valid Pet pet, BindingResult result, Owner owner,
            ModelMap model) {
        checkOwnership(this.pets.findById(petId), owner.getId());
        pet.setId(petId);
        if (result.hasErrors()) {
            pet.setOwner(owner);
            model.put("pet", pet);
            return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
        } else {
            owner.addPet(pet);
            this.pets.save(pet);
            return "redirect:/owners/{ownerId}";
        }
    }
```

### 9.20. VisitController.VisitController

Origem: `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` → `VisitController` em `refs/heads/1.5.x`.

Destino: `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` → `VisitController` na V2.

O fluxo MVC recompõe o Owner remoto e valida o vínculo do Pet antes de criar a visita.

Original:

```java
@Autowired
    public VisitController(VisitRepository visits, PetRepository pets) {
        this.visits = visits;
        this.pets = pets;
    }
```

V2:

```java
@Autowired
    public VisitController(VisitRepository visits, PetRepository pets, OwnerRepository owners) {
        this.visits = visits;
        this.pets = pets;
        this.owners = owners;
    }
```

### 9.21. VisitController.setAllowedFields

Origem: `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` → `setAllowedFields` em `refs/heads/1.5.x`.

Destino: `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` → `setAllowedFields` na V2.

O fluxo MVC recompõe o Owner remoto e valida o vínculo do Pet antes de criar a visita.

Original:

```java
@InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields("id");
    }
```

V2:

```java
@InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setAllowedFields("date", "description");
    }
```

### 9.22. VisitController.loadPetWithVisit

Origem: `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` → `loadPetWithVisit` em `refs/heads/1.5.x`.

Destino: `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` → `loadPetWithVisit` na V2.

O fluxo MVC recompõe o Owner remoto e valida o vínculo do Pet antes de criar a visita.

Original:

```java
/**
     * Called before each and every @RequestMapping annotated method.
     * 2 goals:
     * - Make sure we always have fresh data
     * - Since we do not use the session scope, make sure that Pet object always has an id
     * (Even though id is not part of the form fields)
     *
     * @param petId
     * @return Pet
     */
    @ModelAttribute("visit")
    public Visit loadPetWithVisit(@PathVariable("petId") int petId, Map<String, Object> model) {
        Pet pet = this.pets.findById(petId);
        model.put("pet", pet);
        Visit visit = new Visit();
        pet.addVisit(visit);
        return visit;
    }
```

V2:

```java
/**
     * Called before each and every @RequestMapping annotated method.
     * 2 goals:
     * - Make sure we always have fresh data
     * - Since we do not use the session scope, make sure that Pet object always has an id
     * (Even though id is not part of the form fields)
     *
     * @param petId
     * @return Pet
     */
    @ModelAttribute("visit")
    public Visit loadPetWithVisit(@PathVariable("ownerId") int ownerId, @PathVariable("petId") int petId,
            Map<String, Object> model) {
        Pet pet = this.pets.findById(petId);
        if (pet == null || !Integer.valueOf(ownerId).equals(pet.getOwnerId())) {
            throw new org.springframework.samples.petclinic.integration.OwnerNotFoundException();
        }
        pet.setOwner(this.owners.findById(ownerId));
        model.put("pet", pet);
        Visit visit = new Visit();
        pet.addVisit(visit);
        return visit;
    }
```

### 9.23. VisitController.initNewVisitForm

Origem: `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` → `initNewVisitForm` em `refs/heads/1.5.x`.

Destino: `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` → `initNewVisitForm` na V2.

O fluxo MVC recompõe o Owner remoto e valida o vínculo do Pet antes de criar a visita.

Original:

```java
@GetMapping("/owners/*/pets/{petId}/visits/new")
    public String initNewVisitForm(@PathVariable("petId") int petId, Map<String, Object> model) {
        return "pets/createOrUpdateVisitForm";
    }
```

V2:

```java
@GetMapping("/owners/{ownerId}/pets/{petId}/visits/new")
    public String initNewVisitForm(@PathVariable("petId") int petId, Map<String, Object> model) {
        return "pets/createOrUpdateVisitForm";
    }
```

### 9.24. PetControllerTests.setup

Origem: `src/test/java/org/springframework/samples/petclinic/owner/PetControllerTests.java` → `setup` em `refs/heads/1.5.x`.

Destino: `src/test/java/org/springframework/samples/petclinic/owner/PetControllerTests.java` → `setup` na V2.

O cenário da V2 utiliza Owner identificado e vínculo explícito com Pet.

Original:

```java
@Before
    public void setup() {
        PetType cat = new PetType();
        cat.setId(3);
        cat.setName("hamster");
        given(this.pets.findPetTypes()).willReturn(Lists.newArrayList(cat));
        given(this.owners.findById(TEST_OWNER_ID)).willReturn(new Owner());
        given(this.pets.findById(TEST_PET_ID)).willReturn(new Pet());

    }
```

V2:

```java
@Before
    public void setup() {
        PetType cat = new PetType();
        cat.setId(3);
        cat.setName("hamster");
        given(this.pets.findPetTypes()).willReturn(Lists.newArrayList(cat));
        Owner owner = new Owner();
        owner.setId(TEST_OWNER_ID);
        Pet pet = new Pet();
        pet.setId(TEST_PET_ID);
        pet.setName("Leo");
        owner.addPet(pet);
        given(this.owners.findById(TEST_OWNER_ID)).willReturn(owner);
        given(this.pets.findById(TEST_PET_ID)).willReturn(pet);

    }
```

### 9.25. VisitControllerTests.init

Origem: `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java` → `init` em `refs/heads/1.5.x`.

Destino: `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java` → `init` na V2.

O cenário da V2 representa o vínculo de Owner remoto com Pet local e a rota concreta.

Original:

```java
@Before
    public void init() {
        given(this.pets.findById(TEST_PET_ID)).willReturn(new Pet());
    }
```

V2:

```java
@Before
    public void init() {
        Owner owner = new Owner();
        owner.setId(1);
        Pet pet = new Pet();
        pet.setId(TEST_PET_ID);
        owner.addPet(pet);
        given(this.owners.findById(1)).willReturn(owner);
        given(this.pets.findById(TEST_PET_ID)).willReturn(pet);
    }
```

### 9.26. VisitControllerTests.testInitNewVisitForm

Origem: `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java` → `testInitNewVisitForm` em `refs/heads/1.5.x`.

Destino: `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java` → `testInitNewVisitForm` na V2.

O cenário da V2 representa o vínculo de Owner remoto com Pet local e a rota concreta.

Original:

```java
@Test
    public void testInitNewVisitForm() throws Exception {
        mockMvc.perform(get("/owners/*/pets/{petId}/visits/new", TEST_PET_ID))
            .andExpect(status().isOk())
            .andExpect(view().name("pets/createOrUpdateVisitForm"));
    }
```

V2:

```java
@Test
    public void testInitNewVisitForm() throws Exception {
        mockMvc.perform(get("/owners/1/pets/{petId}/visits/new", TEST_PET_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("pets/createOrUpdateVisitForm"));
    }
```

### 9.27. VisitControllerTests.testProcessNewVisitFormSuccess

Origem: `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java` → `testProcessNewVisitFormSuccess` em `refs/heads/1.5.x`.

Destino: `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java` → `testProcessNewVisitFormSuccess` na V2.

O cenário da V2 representa o vínculo de Owner remoto com Pet local e a rota concreta.

Original:

```java
@Test
    public void testProcessNewVisitFormSuccess() throws Exception {
        mockMvc.perform(post("/owners/*/pets/{petId}/visits/new", TEST_PET_ID)
            .param("name", "George")
            .param("description", "Visit Description")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(view().name("redirect:/owners/{ownerId}"));
    }
```

V2:

```java
@Test
    public void testProcessNewVisitFormSuccess() throws Exception {
        mockMvc.perform(post("/owners/1/pets/{petId}/visits/new", TEST_PET_ID)
                .param("name", "George")
                .param("description", "Visit Description"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/owners/{ownerId}"));
    }
```

### 9.28. VisitControllerTests.testProcessNewVisitFormHasErrors

Origem: `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java` → `testProcessNewVisitFormHasErrors` em `refs/heads/1.5.x`.

Destino: `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java` → `testProcessNewVisitFormHasErrors` na V2.

O cenário da V2 representa o vínculo de Owner remoto com Pet local e a rota concreta.

Original:

```java
@Test
    public void testProcessNewVisitFormHasErrors() throws Exception {
        mockMvc.perform(post("/owners/*/pets/{petId}/visits/new", TEST_PET_ID)
            .param("name", "George")
        )
            .andExpect(model().attributeHasErrors("visit"))
            .andExpect(status().isOk())
            .andExpect(view().name("pets/createOrUpdateVisitForm"));
    }
```

V2:

```java
@Test
    public void testProcessNewVisitFormHasErrors() throws Exception {
        mockMvc.perform(post("/owners/1/pets/{petId}/visits/new", TEST_PET_ID)
                .param("name", "George"))
                .andExpect(model().attributeHasErrors("visit"))
                .andExpect(status().isOk())
                .andExpect(view().name("pets/createOrUpdateVisitForm"));
    }
```

### 9.29. ClinicServiceTests.shouldFindPetWithCorrectId

Origem: `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java` → `shouldFindPetWithCorrectId` em `refs/heads/1.5.x`.

Destino: `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java` → `shouldFindPetWithCorrectId` na V2.

A asserção da V2 verifica ownerId local em vez de navegar em associação JPA para Owner.

Original:

```java
@Test
    public void shouldFindPetWithCorrectId() {
        Pet pet7 = this.pets.findById(7);
        assertThat(pet7.getName()).startsWith("Samantha");
        assertThat(pet7.getOwner().getFirstName()).isEqualTo("Jean");

    }
```

V2:

```java
@Test
    public void shouldFindPetWithCorrectId() {
        Pet pet7 = this.pets.findById(7);
        assertThat(pet7.getName()).startsWith("Samantha");
        assertThat(pet7.getOwnerId()).isEqualTo(6);

    }
```

### 9.30. OwnerRepository.findByLastNameStartingWithIgnoreCaseOrderByLastNameAscIdAsc

Origem: `src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java` → `findByLastName` em `refs/heads/1.5.x`.

Destino: `owners-service/src/main/java/br/edu/tcc/owners/OwnerRepository.java` → `findByLastNameStartingWithIgnoreCaseOrderByLastNameAscIdAsc` na V2.

A busca persiste no serviço; a V2 já usa prefixo sem distinguir maiúsculas e ordenação explícita. Não é uma tradução literal da JPQL original.

Original:

```java
/**
     * Retrieve {@link Owner}s from the data store by last name, returning all owners
     * whose last name <i>starts</i> with the given name.
     * @param lastName Value to search for
     * @return a Collection of matching {@link Owner}s (or an empty Collection if none
     * found)
     */
    @Query("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets WHERE owner.lastName LIKE :lastName%")
    @Transactional(readOnly = true)
    Collection<Owner> findByLastName(@Param("lastName") String lastName);
```

V2:

```java
List<Owner> findByLastNameStartingWithIgnoreCaseOrderByLastNameAscIdAsc(String lastName);
```

## 10. Preservação metodológica, hashes e validação

`docs/metricas/v2` contém a observação histórica: CSVs, logs, relatórios, hashes e evidências devem continuar representando a coleta original. Nada nessa pasta foi atualizado por esta organização. `docs/baseline`, `docs/historico`, a aplicação histórica `owners-pets-service` e os coletores/analisadores de CK também foram preservados.

Os arquivos Java organizados terão hashes diferentes dos registrados em `docs/metricas/v2/fontes-avaliadas.json`, mesmo mantendo a lógica. Os hashes históricos não devem ser substituídos pelos novos. Os valores de CK anteriores continuam vinculados aos fontes e ao checkpoint documentados na coleta original, e não são apresentados como uma medição deste estado organizado. Comentários e formatação podem influenciar medidas baseadas em linhas; não se presume igualdade de todas as métricas CK.

Nenhum coletor CK foi executado. Uma futura coleta, caso se queira medir especificamente este estado, deve produzir um conjunto separado e identificado, preservando integralmente os resultados anteriores. Não é necessária nova coleta para concluir esta tarefa de apresentação.

A validação desta intervenção usa análise sintática e comparação estrutural dos 24 arquivos contra a cópia de trabalho anterior, desconsiderando comentários, espaços e ordem de membros. Blocos de uma única instrução são normalizados para comparar a expansão de if/for sem mudança da instrução. A conferência dos inicializadores complementa essa análise: ordenar declarações não autoriza mudar a ordem de execução de inicializadores dependentes.

As compilações e os testes foram executados em uma cópia isolada, com JDK 8 para o monólito e JDK 17 para Owners, Gateway e o script Java. Maven 3.9.11 executou `-B -o clean package`, com `-Dcobertura.skip=true` no legado, conforme as opções de `scripts/build.ps1`. O legado registrou 47 testes, zero falhas/erros e 1 ignorado; Owners registrou 6 testes e Gateway 1, todos aprovados. Total: 53 aprovados e 1 ignorado. `MigrateOwners.java` compilou com `javac -encoding UTF-8`; não foi executado. Os testes existentes de Owners usam H2 em memória e o legado tem propriedades de teste próprias.

Esses resultados não substituem as evidências acadêmicas anteriores. Não foram reexecutados a bateria HTTP externa, os ensaios de durabilidade, a migração de bancos nem CK. A comparação estrutural também normaliza concatenações de literais de texto constantes: a fixture JSON longa foi apenas dividida em dois literais, com conteúdo idêntico. Logs novos e o relatório final acompanham a entrega fora de `docs/metricas/v2`.


## 11. Intervenção posterior: centralização de erros da integração

Em 03/11/2026 foi realizada, por decisão arquitetural do autor, uma simplificação posterior à primeira V2 e à sua organização visual. Os três arquivos `integration/OwnerNotFoundException.java`, `OwnerValidationException.java` e `OwnersUnavailableException.java` foram removidos. Não se deve ler essa mudança como parte da extração original descrita acima.

Antes, `OwnersClient.translate` selecionava essas três classes. Duas usavam `@ResponseStatus` para 404/400; `IntegrationErrorHandler.unavailable` devolvia 503 e `ownersUnavailable`. Agora, o cliente continua com HTTP, encoding, payload e os mesmos timeouts, mas delega as falhas capturadas de `RestClientException | HttpMessageConversionException` a `IntegrationErrorHandler.remoteFailure`. A classificação dos status e a resposta MVC ficam no handler. Não há captura global de RuntimeException nem uso de ResponseStatusException no legado.

PetController.checkOwnership e VisitController.loadPetWithVisit chamam a fábrica `IntegrationErrorHandler.notFound` para o vínculo inválido, preservando HTTP 404. O método `handle` usa `sendError` para 404/400 com os motivos anteriores, mantendo o despacho das páginas MVC, e retorna `ModelAndView("ownersUnavailable")` com 503 nas falhas de disponibilidade. Um erro de programação sem marcador continua no tratamento original.

Um único tipo interno, `IntegrationFailure`, transporta status e causa e delimita o advice. Seu construtor é privado; consumidores utilizam as fábricas do handler. Isso evita interceptar falhas de outros clientes HTTP ou erros gerais de programação. Não foram recriadas três exceções dentro do arquivo: há apenas um marcador com dados, classificação compartilhada e um handler de resposta.

Origem deste passo: `src/main/java/org/springframework/samples/petclinic/integration/OwnersClient.java`, método `translate`, cenário B. Destino da classificação: mesmo pacote, `IntegrationErrorHandler.remoteFailure`, cenário C. O trecho anterior real preservado no snapshot B era:

```java
    private RuntimeException translate(RuntimeException ex) {
        if (ex instanceof HttpStatusCodeException) {
            HttpStatus status = ((HttpStatusCodeException) ex).getStatusCode();
            if (status == HttpStatus.NOT_FOUND) {
                return new OwnerNotFoundException();
            }
            if (status == HttpStatus.BAD_REQUEST) {
                return new OwnerValidationException();
            }
        }
        return new OwnersUnavailableException(ex);
    }
```

Implementação central atual: `src/main/java/org/springframework/samples/petclinic/integration/IntegrationErrorHandler.java`, com as fábricas, handler MVC e tipo interno:

```java
package org.springframework.samples.petclinic.integration;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.servlet.ModelAndView;

// Componente da coexistência: centraliza classificação e respostas de erros da integração Owner.
@ControllerAdvice
public class IntegrationErrorHandler {
    // Antes, OwnersClient.translate escolhia três exceções. A decisão agora pertence a este ponto central.
    // O chamador delimita a origem: somente falhas HTTP/conversão capturadas na fronteira remota chegam aqui.
    public static IntegrationFailure remoteFailure(RuntimeException cause) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        if (cause instanceof HttpStatusCodeException) {
            HttpStatus remoteStatus = ((HttpStatusCodeException) cause).getStatusCode();
            if (remoteStatus == HttpStatus.NOT_FOUND || remoteStatus == HttpStatus.BAD_REQUEST) {
                status = remoteStatus;
            }
        }
        return new IntegrationFailure(status, cause);
    }

    // Antes, os controllers lançavam uma exceção exclusiva para o vínculo inválido de Pet e Owner.
    public static IntegrationFailure notFound() {
        return new IntegrationFailure(HttpStatus.NOT_FOUND, null);
    }

    // Antes, o advice só tratava indisponibilidade; 400/404 dependiam de anotações nas exceções removidas.
    // sendError mantém o despacho das páginas de erro MVC e os motivos usados anteriormente.
    @ExceptionHandler(IntegrationFailure.class)
    public ModelAndView handle(IntegrationFailure failure, HttpServletResponse response) throws IOException {
        HttpStatus status = failure.getStatus();
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            response.setStatus(status.value());
            return new ModelAndView("ownersUnavailable");
        }
        String reason = status == HttpStatus.NOT_FOUND ? "Owner or Pet not found" : "Invalid owner data";
        response.sendError(status.value(), reason);
        return new ModelAndView();
    }

    // Um único tipo interno transporta status e causa e delimita o advice. Sem esse marcador,
    // capturar RuntimeException ou falhas HTTP globais poderia mascarar erros fora da integração.
    public static final class IntegrationFailure extends RuntimeException {
        private final HttpStatus status;

        private IntegrationFailure(HttpStatus status, Throwable cause) {
            super("Owner integration failure: " + status.value(), cause);
            this.status = status;
        }

        public HttpStatus getStatus() {
            return this.status;
        }
    }
}
```

As portas, rotas do Gateway, contratos REST, validações e modelos foram preservados. Owner continua persistido somente em Owners; Pets e Visits continuam no legado. Os comentários históricos de 1.5.x nos controllers foram mantidos e os pontos alterados receberam notas curtas sobre o tratamento anterior.

As três suítes Maven foram executadas novamente: monólito com JDK 8, 60 aprovados e 1 ignorado; Owners com JDK 17, 6 aprovados; Gateway com JDK 17, 1 aprovado. Total: 67 aprovados e 1 ignorado, zero falhas e erros. Os novos cenários cobrem 404, 400, indisponibilidade, timeout, conexão, conversão, a view 503, vínculo local e erros não pertencentes à integração.

A aceitação integrada passou com 59 checks HTTP, incluindo prontidão, persistência após interrupção/reinicialização, negócios de Owner/Pet/Visit/Vet, rotas, 404, 400 remoto, 500 interno e página 503. A migração de uma fixture histórica passou, preservando IDs, referências e hashes da origem e recusando destino existente.

Nova coleta CK: `../metricas/v2-centralizacao-erros/relatorio.md`, com A = monólito original 1.5.x, B = V2 anterior a esta centralização e C = V2 centralizada. As médias do monólito são:

| Cenário | Tipos CK | CBO | WMC | RFC | LCOM | LCOM* |
| --- | --- | --- | --- | --- | --- | --- |
| A | 24 | 6,666667 | 4,916667 | 3,458333 | 5,875000 | 0,256618 |
| B | 30 | 6,166667 | 4,866667 | 4,366667 | 5,266667 | 0,227258 |
| C | 28 | 6,571429 | 5,357143 | 4,928571 | 5,750000 | 0,236348 |

O CK conta o tipo interno como uma classe adicional. Três classes removidas e uma interna adicionada resultam em dois tipos a menos. O cliente apresentou queda local de CBO 13→8; o handler passou de 4→7. O CBO médio do monólito aumentou; a redução de arquivos não demonstra melhoria geral. O relatório detalha pares equivalentes, dependências, coesão, somatórios, médias e limitações, inclusive uma diferença de coesão em Owners devida ao checkpoint de organização anterior e reproduzida por controle histórico.

`docs/metricas/v2` e baselines permanecem intactos. As novas fontes, hashes, CSVs, controles e evidências estão em `docs/metricas/v2-centralizacao-erros`. Nenhum resultado anterior foi sobrescrito. A migração validada usa fixture, e timeout/conversão são simulados nos testes; os limites estão explicitados no novo relatório.

## Apêndice A. Estrutura original integral das classes de dados e repositório

As cópias integrais ficam aqui para manter os fontes atuais legíveis. Elas permitem consultar também os mapeamentos removidos e os acessores sem equivalente direto no serviço, como `BaseEntity.setId`, `BaseEntity.isNew` e a coleção/métodos de Pets do Owner original. `Person` e `BaseEntity` continuam no legado; não foram apagados nem editados por esta tarefa. Os métodos de Pets continuam ligados à composição no legado, sem destino na entidade de Owners.

### src/main/java/org/springframework/samples/petclinic/owner/Owner.java

Origem integral: `refs/heads/1.5.x:src/main/java/org/springframework/samples/petclinic/owner/Owner.java`.

```java
/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Digits;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.support.MutableSortDefinition;
import org.springframework.beans.support.PropertyComparator;
import org.springframework.core.style.ToStringCreator;
import org.springframework.samples.petclinic.model.Person;

/**
 * Simple JavaBean domain object representing an owner.
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 */
@Entity
@Table(name = "owners")
public class Owner extends Person {
    @Column(name = "address")
    @NotEmpty
    private String address;

    @Column(name = "city")
    @NotEmpty
    private String city;

    @Column(name = "telephone")
    @NotEmpty
    @Digits(fraction = 0, integer = 10)
    private String telephone;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    private Set<Pet> pets;


    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return this.city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getTelephone() {
        return this.telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    protected Set<Pet> getPetsInternal() {
        if (this.pets == null) {
            this.pets = new HashSet<>();
        }
        return this.pets;
    }

    protected void setPetsInternal(Set<Pet> pets) {
        this.pets = pets;
    }

    public List<Pet> getPets() {
        List<Pet> sortedPets = new ArrayList<>(getPetsInternal());
        PropertyComparator.sort(sortedPets, new MutableSortDefinition("name", true, true));
        return Collections.unmodifiableList(sortedPets);
    }

    public void addPet(Pet pet) {
        if (pet.isNew()) {
            getPetsInternal().add(pet);
        }
        pet.setOwner(this);
    }

    /**
     * Return the Pet with the given name, or null if none found for this Owner.
     *
     * @param name to test
     * @return true if pet name is already in use
     */
    public Pet getPet(String name) {
        return getPet(name, false);
    }

    /**
     * Return the Pet with the given name, or null if none found for this Owner.
     *
     * @param name to test
     * @return true if pet name is already in use
     */
    public Pet getPet(String name, boolean ignoreNew) {
        name = name.toLowerCase();
        for (Pet pet : getPetsInternal()) {
            if (!ignoreNew || !pet.isNew()) {
                String compName = pet.getName();
                compName = compName.toLowerCase();
                if (compName.equals(name)) {
                    return pet;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return new ToStringCreator(this)

            .append("id", this.getId())
            .append("new", this.isNew())
            .append("lastName", this.getLastName())
            .append("firstName", this.getFirstName())
            .append("address", this.address)
            .append("city", this.city)
            .append("telephone", this.telephone)
            .toString();
    }
}
```

### src/main/java/org/springframework/samples/petclinic/model/Person.java

Origem integral: `refs/heads/1.5.x:src/main/java/org/springframework/samples/petclinic/model/Person.java`.

```java
/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Simple JavaBean domain object representing an person.
 *
 * @author Ken Krebs
 */
@MappedSuperclass
public class Person extends BaseEntity {

    @Column(name = "first_name")
    @NotEmpty
    private String firstName;

    @Column(name = "last_name")
    @NotEmpty
    private String lastName;

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

}
```

### src/main/java/org/springframework/samples/petclinic/model/BaseEntity.java

Origem integral: `refs/heads/1.5.x:src/main/java/org/springframework/samples/petclinic/model/BaseEntity.java`.

```java
/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.model;

import java.io.Serializable;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Simple JavaBean domain object with an id property. Used as a base class for objects
 * needing this property.
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 */
@MappedSuperclass
public class BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean isNew() {
        return this.id == null;
    }

}
```

### src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java

Origem integral: `refs/heads/1.5.x:src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java`.

```java
/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.util.Collection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository class for <code>Owner</code> domain objects All method names are compliant with Spring Data naming
 * conventions so this interface can easily be extended for Spring Data See here: http://static.springsource.org/spring-data/jpa/docs/current/reference/html/jpa.repositories.html#jpa.query-methods.query-creation
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 */
public interface OwnerRepository extends Repository<Owner, Integer> {

    /**
     * Retrieve {@link Owner}s from the data store by last name, returning all owners
     * whose last name <i>starts</i> with the given name.
     * @param lastName Value to search for
     * @return a Collection of matching {@link Owner}s (or an empty Collection if none
     * found)
     */
    @Query("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets WHERE owner.lastName LIKE :lastName%")
    @Transactional(readOnly = true)
    Collection<Owner> findByLastName(@Param("lastName") String lastName);

    /**
     * Retrieve an {@link Owner} from the data store by id.
     * @param id the id to search for
     * @return the {@link Owner} if found
     */
    @Query("SELECT owner FROM Owner owner left join fetch owner.pets WHERE owner.id =:id")
    @Transactional(readOnly = true)
    Owner findById(@Param("id") Integer id);

    /**
     * Save an {@link Owner} to the data store, either inserting or updating it.
     * @param owner the {@link Owner} to save
     */
    void save(Owner owner);


}
```

### Mapeamento original de Pet.owner e destino na V2

Origem: `src/main/java/org/springframework/samples/petclinic/owner/Pet.java`, atributo `owner`, em 1.5.x. Destino: mesmo arquivo, atributos `ownerId` e `owner`, na V2. O trecho antigo foi recuperado do fonte original; o atual foi extraído do fonte organizado.

```java
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Owner owner;
```

```java
@Column(name = "owner_id", nullable = false)
    private Integer ownerId;

@javax.persistence.Transient
    private Owner owner;
```

## Apêndice B. Métodos originais removidos sem destino direto

Os métodos abaixo já não estavam presentes na V2 antes desta organização. São preservados integralmente, com suas anotações, sem reintroduzi-los na suíte executável. Não há equivalência direta um a um: os testes REST `OwnerApiTests.historicalOwnerAndPrefixSearch` e `createAndUpdate` exercitam busca/criação/atualização em outra camada; `ClinicServiceTests` usa um cliente remoto simulado, sem validar persistência real de Owner no legado.

### shouldFindOwnersByLastName

Origem: `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java` → `shouldFindOwnersByLastName`, branch `refs/heads/1.5.x`. Destino direto: inexistente na V2.

```java
@Test
    public void shouldFindOwnersByLastName() {
        Collection<Owner> owners = this.owners.findByLastName("Davis");
        assertThat(owners.size()).isEqualTo(2);

        owners = this.owners.findByLastName("Daviss");
        assertThat(owners.isEmpty()).isTrue();
    }
```

### shouldInsertOwner

Origem: `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java` → `shouldInsertOwner`, branch `refs/heads/1.5.x`. Destino direto: inexistente na V2.

```java
@Test
    @Transactional
    public void shouldInsertOwner() {
        Collection<Owner> owners = this.owners.findByLastName("Schultz");
        int found = owners.size();

        Owner owner = new Owner();
        owner.setFirstName("Sam");
        owner.setLastName("Schultz");
        owner.setAddress("4, Evans Street");
        owner.setCity("Wollongong");
        owner.setTelephone("4444444444");
        this.owners.save(owner);
        assertThat(owner.getId().longValue()).isNotEqualTo(0);

        owners = this.owners.findByLastName("Schultz");
        assertThat(owners.size()).isEqualTo(found + 1);
    }
```

### shouldUpdateOwner

Origem: `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java` → `shouldUpdateOwner`, branch `refs/heads/1.5.x`. Destino direto: inexistente na V2.

```java
@Test
    @Transactional
    public void shouldUpdateOwner() {
        Owner owner = this.owners.findById(1);
        String oldLastName = owner.getLastName();
        String newLastName = oldLastName + "X";

        owner.setLastName(newLastName);
        this.owners.save(owner);

        // retrieving new name from database
        owner = this.owners.findById(1);
        assertThat(owner.getLastName()).isEqualTo(newLastName);
    }
```

## Apêndice C. Arquivos Java incluídos nesta organização

- `gateway/src/main/java/br/edu/tcc/gateway/GatewayApplication.java`
- `gateway/src/test/java/br/edu/tcc/gateway/GatewayRoutesTests.java`
- `owners-service/src/main/java/br/edu/tcc/owners/Owner.java`
- `owners-service/src/main/java/br/edu/tcc/owners/OwnerController.java`
- `owners-service/src/main/java/br/edu/tcc/owners/OwnerRepository.java`
- `owners-service/src/main/java/br/edu/tcc/owners/OwnersApplication.java`
- `owners-service/src/test/java/br/edu/tcc/owners/OwnerApiTests.java`
- `scripts/MigrateOwners.java`
- `src/main/java/org/springframework/samples/petclinic/integration/IntegrationErrorHandler.java`
- `src/main/java/org/springframework/samples/petclinic/integration/OwnerNotFoundException.java`
- `src/main/java/org/springframework/samples/petclinic/integration/OwnerValidationException.java`
- `src/main/java/org/springframework/samples/petclinic/integration/OwnersClient.java`
- `src/main/java/org/springframework/samples/petclinic/integration/OwnersUnavailableException.java`
- `src/main/java/org/springframework/samples/petclinic/owner/Owner.java`
- `src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java`
- `src/main/java/org/springframework/samples/petclinic/owner/Pet.java`
- `src/main/java/org/springframework/samples/petclinic/owner/PetController.java`
- `src/main/java/org/springframework/samples/petclinic/owner/PetRepository.java`
- `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java`
- `src/main/java/org/springframework/samples/petclinic/system/LegacyDatabaseInitializer.java`
- `src/test/java/org/springframework/samples/petclinic/integration/OwnersClientTests.java`
- `src/test/java/org/springframework/samples/petclinic/owner/PetControllerTests.java`
- `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java`
- `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java`
