package org.springframework.samples.petclinic.system;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.ResourceAccessException;

public class OwnersPetsExceptionHandlerTests {

    @Controller
    public static class TestController {

        @GetMapping("/test/owners-pets-unavailable")
        public String unavailable() {
            throw new ResourceAccessException("Connection refused");
        }
    }

    @Test
    public void shouldReturn503WhenServiceIsUnavailable() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new OwnersPetsExceptionHandler())
                .build();

        mockMvc.perform(get("/test/owners-pets-unavailable"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("error"))
                .andExpect(model().attribute(
                        "message",
                        "O servico de proprietarios e animais esta temporariamente indisponivel. Tente novamente mais tarde."));
    }
}