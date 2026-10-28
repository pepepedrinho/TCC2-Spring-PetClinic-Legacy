# Inspecao da baseline e decisoes V2
Baseline: c36452a2c34443ae26b4ecbba4f149906af14717 (refs/heads/1.5.x).
Anterior consultada: a3e74c1 (refs/heads/tcc2-strangler-fig).
Nao foram encontrados AGENTS.md no repositorio.

Owner herda Person/BaseEntity, persiste cadastro e agrega Pets por JPA. OwnerRepository executa pesquisa, detalhe e gravacao; OwnerController renderiza Thymeleaf. Pet/PetRepository/PetController/PetType/PetTypeFormatter/PetValidator sao responsabilidades remanescentes, assim como Visit/VisitRepository/VisitController e Vets.

V2: Owner deixa de ser entidade no legado. OwnerRepository torna-se adaptador HTTP com composicao local de Pets. Pet persiste owner_id escalar e mantem Owner transitorio para as views. Pets e visitas continuam JPA no legado. Controllers/templates historicos sao preservados sempre que possivel. Owner nao sera excluido pela API (409 para existente), politica conservadora que impede orfaos e corridas distribuídas sem dependencia HTTP inversa. Identificadores nao sao reutilizados.

Gateway recuperado da branch anterior sem reimplementacao. Somente rota Owners sera alterada. Boot 3.5.16 e Cloud 2025.0.3 sao as versoes ja utilizadas; legado permanece Boot 1.5.4 e Java 8.

Novos bancos V2 terao caminhos separados dos bancos anteriores. Inicializacao em bancos vazios, sem DROP/reseeding em reinicios. CSVs baseline preservados byte a byte. Ferramenta anterior documentada como CK 0.7.1; nao ha scripts de coleta versionados na branch anterior.
