package br.com.brunolopes.conciliacao;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @GetMapping("/api/status")
    public StatusResponse consultarStatus() {
        return new StatusResponse("conciliacao-api-java", "OK");
    }
}
