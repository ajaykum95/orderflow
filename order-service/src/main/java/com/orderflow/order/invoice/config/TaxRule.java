package com.orderflow.order.invoice.config;

import java.math.BigDecimal;

// एक टैक्स का नियम, जैसे  CGST 9%  या  GST 18%
public class TaxRule {

    private String name;
    private BigDecimal rate;   // प्रतिशत में: 9 का मतलब 9%

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
