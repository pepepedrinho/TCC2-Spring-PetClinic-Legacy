package br.edu.tcc.gateway;

import java.io.IOException;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Reports a failed connection to Owners as service unavailability. */
@Component
class OwnersAvailabilityGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> chain.filter(exchange)
                .onErrorMap(IOException.class, ex -> new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "Owners service is unavailable", ex));
    }
}
