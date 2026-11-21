package com.orderflow.payment.chaos;

import com.orderflow.payment.dto.PaymentDtos.ChaosRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chaos")
public class ChaosController {

    private final ChaosSettings chaos;

    public ChaosController(ChaosSettings chaos) {
        this.chaos = chaos;
    }

    @GetMapping
    public Map<String, Object> get() {
        return view();
    }

    @PutMapping
    public Map<String, Object> update(@Valid @RequestBody ChaosRequest req) {
        chaos.update(req.failureRate(), req.slowRate(), req.slowDelayMs());
        return view();
    }

    private Map<String, Object> view() {
        return Map.of("failureRate", chaos.getFailureRate(),
                      "slowRate", chaos.getSlowRate(),
                      "slowDelayMs", chaos.getSlowDelayMs());
    }
}
