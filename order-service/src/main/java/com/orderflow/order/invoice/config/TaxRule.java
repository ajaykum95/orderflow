package com.orderflow.order.invoice.config;

import java.math.BigDecimal;

public class TaxRule {

    private String name;
    private BigDecimal rate;

    public TaxRule() {}

    public TaxRule(String name, BigDecimal rate) {
        this.name = name;
        this.rate = rate;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
}
