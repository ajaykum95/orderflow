package com.orderflow.order.invoice.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// Invoice एक बार बनने के बाद बदलता नहीं (कोई setter नहीं)।
// दुकान का नाम/GSTIN भी invoice में ही रखा है, ताकि config बाद में बदले तब भी पुराना invoice वैसा ही रहे।
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    // एक order का एक ही invoice; database खुद दोहराव रोकता है
    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private String customerEmail;

    private String currency;
    private String sellerName;
    private String sellerAddress;
    private String sellerGstin;

    @Column(precision = 12, scale = 2)
    private BigDecimal taxableAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalTax;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private Instant issuedAt;

    @ElementCollection
    @CollectionTable(name = "invoice_lines", joinColumns = @JoinColumn(name = "invoice_id"))
    @OrderColumn(name = "line_no")
    private List<InvoiceLine> lines = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "invoice_taxes", joinColumns = @JoinColumn(name = "invoice_id"))
    @OrderColumn(name = "tax_no")
    private List<InvoiceTax> taxes = new ArrayList<>();

    protected Invoice() {}

    public Invoice(String invoiceNumber, Long orderId, String customerEmail, String currency,
                   String sellerName, String sellerAddress, String sellerGstin,
                   BigDecimal taxableAmount, BigDecimal totalTax, BigDecimal totalAmount, Instant issuedAt) {
        this.invoiceNumber = invoiceNumber;
        this.orderId = orderId;
        this.customerEmail = customerEmail;
        this.currency = currency;
        this.sellerName = sellerName;
        this.sellerAddress = sellerAddress;
        this.sellerGstin = sellerGstin;
        this.taxableAmount = taxableAmount;
        this.totalTax = totalTax;
        this.totalAmount = totalAmount;
        this.issuedAt = issuedAt;
    }

    public void addLine(InvoiceLine line) { lines.add(line); }
    public void addTax(InvoiceTax tax) { taxes.add(tax); }

    public Long getId() { return id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public Long getOrderId() { return orderId; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCurrency() { return currency; }
    public String getSellerName() { return sellerName; }
    public String getSellerAddress() { return sellerAddress; }
    public String getSellerGstin() { return sellerGstin; }
    public BigDecimal getTaxableAmount() { return taxableAmount; }
    public BigDecimal getTotalTax() { return totalTax; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getIssuedAt() { return issuedAt; }
    public List<InvoiceLine> getLines() { return lines; }
    public List<InvoiceTax> getTaxes() { return taxes; }
}
