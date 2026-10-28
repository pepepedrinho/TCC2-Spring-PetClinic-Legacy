package br.edu.tcc.gateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesTests {
    @Autowired RouteDefinitionLocator routes;
    @Test void preservesOwnersOnlyRoutesAndHostFilter() {
        var definitions=routes.getRouteDefinitions().collectList().block();
        assertThat(definitions).hasSize(2);
        var owners=definitions.stream().filter(r -> r.getId().equals("owners-service")).findFirst().orElseThrow();
        assertThat(owners.getOrder()).isEqualTo(-1);
        assertThat(owners.getUri().toString()).isEqualTo("http://localhost:8082");
        assertThat(owners.getPredicates().get(0).getArgs().values()).containsExactly("/api/owners","/api/owners/**");
        assertThat(owners.getFilters().get(0).getName()).isEqualTo("PreserveHostHeader");
        var legacy=definitions.stream().filter(r -> r.getId().equals("petclinic-monolith")).findFirst().orElseThrow();
        assertThat(legacy.getOrder()).isEqualTo(0);
        assertThat(legacy.getPredicates().get(0).getArgs().values()).containsExactly("/**");
    }
}
