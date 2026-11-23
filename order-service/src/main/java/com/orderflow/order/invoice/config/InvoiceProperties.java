package com.orderflow.order.invoice.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.invoice")
public class InvoiceProperties {

    private String sellerName = "OrderFlow Demo Store";
    private String sellerAddress = "Demo Address, India";
    private String sellerGstin = "00AAAAA0000A1Z5";
    private String currency = "INR";
    private List<TaxRule> taxRules = new ArrayList<>(List.of(new TaxRule("GST", new BigDecimal("18"))));

    @PostConstruct
    void validate() {
        if (taxRules != null) {
            for (TaxRule r : taxRules) {
                if (r.getName() == null || r.getName().isBlank() || r.getRate() == null
                        || r.getRate().signum() < 0 || r.getRate().compareTo(new BigDecimal("100")) > 0) {
                    throw new IllegalStateException("Each rule in app.invoice.tax-rules must have a name and a rate between 0 and 100");
                }
            }
        }
    }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String v) { this.sellerName = v; }
    public String getSellerAddress() { return sellerAddress; }
    public void setSellerAddress(String v) { this.sellerAddress = v; }
    public String getSellerGstin() { return sellerGstin; }
    public void setSellerGstin(String v) { this.sellerGstin = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }
    public List<TaxRule> getTaxRules() { return taxRules; }
    public void setTaxRules(List<TaxRule> v) { this.taxRules = v; }
}
