package br.com.brunolopes.conciliacao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StatusControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveInformarStatusDaApiEmJson() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.aplicacao")
                        .value("conciliacao-api-java"))
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void devePreservar405ParaMetodoHttpNaoPermitido() throws Exception {
        mockMvc.perform(post("/api/status"))
                .andExpect(status().isMethodNotAllowed());
    }
}
