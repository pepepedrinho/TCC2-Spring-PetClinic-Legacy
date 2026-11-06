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
