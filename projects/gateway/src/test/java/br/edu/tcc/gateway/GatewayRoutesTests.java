package br.edu.tcc.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

import static org.assertj.core.api.Assertions.assertThat;

// Teste novo da coexistência: verifica prioridade, destinos e filtro das rotas configuradas do Gateway.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesTests {
    @Autowired
    RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void preservesOwnersOnlyRoutesAndHostFilter() {
        var routeDefinitions = routeDefinitionLocator.getRouteDefinitions().collectList().block();
        assertThat(routeDefinitions).hasSize(2);
        var ownersRouteDefinition = routeDefinitions.stream().filter(r -> r.getId().equals("owners-service"))
                .findFirst().orElseThrow();
        assertThat(ownersRouteDefinition.getOrder()).isEqualTo(-1);
        assertThat(ownersRouteDefinition.getUri().toString()).isEqualTo("http://localhost:8082");
        assertThat(ownersRouteDefinition.getPredicates().get(0).getArgs().values()).containsExactly("/api/owners",
                "/api/owners/**");
        assertThat(ownersRouteDefinition.getFilters().get(0).getName()).isEqualTo("PreserveHostHeader");
        var legacyRouteDefinition = routeDefinitions.stream().filter(r -> r.getId().equals("petclinic-monolith"))
                .findFirst().orElseThrow();
        assertThat(legacyRouteDefinition.getOrder()).isEqualTo(0);
        assertThat(legacyRouteDefinition.getPredicates().get(0).getArgs().values()).containsExactly("/**");
    }
}
