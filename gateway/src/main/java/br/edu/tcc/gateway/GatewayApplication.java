package br.edu.tcc.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Componente novo da coexistência: inicia o Gateway; as rotas configuradas encaminham /api/owners ao
// serviço e os demais caminhos ao legado.
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
