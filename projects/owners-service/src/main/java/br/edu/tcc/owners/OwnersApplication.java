package br.edu.tcc.owners;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Componente novo da extração: inicia a aplicação independente
// que mantém exclusivamente os dados de Owner.
@SpringBootApplication
public class OwnersApplication {

    public static void main(String[] args) {
        SpringApplication.run(OwnersApplication.class, args);
    }
}
