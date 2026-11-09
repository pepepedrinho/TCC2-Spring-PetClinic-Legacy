package br.edu.tcc.gateway;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesTests {
    @Autowired
    RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void routesOwnerWebAndApiAheadOfLegacyCatchAll() {
        Map<String, RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectMap(RouteDefinition::getId).block();
        assertThat(routes).containsOnlyKeys("owners-service", "owners-web", "internal-block", "petclinic-monolith");

        assertOwnersRoute(routes.get("owners-service"), "/api/owners", "/api/owners/**");
        assertOwnersRoute(routes.get("owners-web"), "/owners", "/owners/find", "/owners/new", "/owners/{ownerId}",
                "/owners/{ownerId}/edit");

        var legacy = routes.get("petclinic-monolith");
        assertThat(legacy.getOrder()).isZero();
        assertThat(legacy.getPredicates().get(0).getArgs().values()).containsExactly("/**");
        assertThat(legacy.getFilters().get(1).getName()).isEqualTo("SetRequestHeader");
        assertThat(legacy.getFilters().get(1).getArgs().values()).containsExactly("X-PetClinic-Gateway", "true");
        var internal = routes.get("internal-block");
        assertThat(internal.getOrder()).isLessThan(legacy.getOrder());
        assertThat(internal.getUri().getScheme()).isEqualTo("no");
        assertThat(internal.getPredicates().get(0).getArgs().values()).containsExactly("/internal/**");
        assertThat(internal.getFilters().get(0).getName()).isEqualTo("SetStatus");
        assertThat(internal.getFilters().get(0).getArgs().values()).containsExactly("404");
    }

    private void assertOwnersRoute(RouteDefinition route, String... paths) {
        assertThat(route.getOrder()).isEqualTo(-1);
        assertThat(route.getUri().toString()).isEqualTo("http://localhost:8082");
        assertThat(route.getPredicates().get(0).getArgs().values()).containsExactly(paths);
        assertThat(route.getFilters().get(0).getName()).isEqualTo("PreserveHostHeader");
        assertThat(route.getFilters().get(1).getName()).isEqualTo("OwnersAvailability");
    }
}
