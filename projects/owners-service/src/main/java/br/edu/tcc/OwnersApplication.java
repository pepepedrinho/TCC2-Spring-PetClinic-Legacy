package br.edu.tcc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.UrlHandlerFilter;

// Componente novo da extração: inicia a aplicação independente
// que mantém exclusivamente os dados de Owner.
@SpringBootApplication
public class OwnersApplication {

    public static void main(String[] args) {
        SpringApplication.run(OwnersApplication.class, args);
    }

    @Bean
    UrlHandlerFilter ownerPageTrailingSlash() {
        return UrlHandlerFilter.trailingSlashHandler("/owners/**").wrapRequest().build();
    }
}
