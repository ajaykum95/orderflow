package com.orderflow.payment.chaos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class ChaosSettings {

    private volatile double failureRate;
    private volatile double slowRate;
    private volatile long slowDelayMs;

    public ChaosSettings(@Value("${payment.chaos.failure-rate}") double failureRate,
                         @Value("${payment.chaos.slow-rate}") double slowRate,
                         @Value("${payment.chaos.slow-delay-ms}") long slowDelayMs) {
        this.failureRate = failureRate;
        this.slowRate = slowRate;
        this.slowDelayMs = slowDelayMs;
    }

    public void maybeMisbehave() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        if (rnd.nextDouble() < failureRate) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Gateway is down (simulated)");
        }
        if (rnd.nextDouble() < slowRate) {
            try {
                Thread.sleep(slowDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public double getFailureRate() { return failureRate; }
    public double getSlowRate() { return slowRate; }
    public long getSlowDelayMs() { return slowDelayMs; }

    public void update(Double failureRate, Double slowRate, Long slowDelayMs) {
        if (failureRate != null) this.failureRate = failureRate;
        if (slowRate != null) this.slowRate = slowRate;
        if (slowDelayMs != null) this.slowDelayMs = slowDelayMs;
    }
}
