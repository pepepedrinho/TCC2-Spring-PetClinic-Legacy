package org.springframework.samples.petclinic.system;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.ResourceAccessException;

@ControllerAdvice
public class OwnersPetsExceptionHandler {

    @ExceptionHandler(ResourceAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String handleServiceUnavailable(
            ResourceAccessException exception, Model model) {

        model.addAttribute("message",
                "O servico de proprietarios e animais esta temporariamente indisponivel. Tente novamente mais tarde.");

        return "error";
    }
}