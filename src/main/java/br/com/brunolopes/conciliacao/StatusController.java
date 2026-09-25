package br.com.brunolopes.conciliacao;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @GetMapping("/api/status")
    public String consultarStatus() {
        return "API de conciliacao funcionando.";
    }
}
