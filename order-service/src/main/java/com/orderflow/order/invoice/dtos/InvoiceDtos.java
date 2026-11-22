package com.orderflow.order.invoice.dtos;

import com.orderflow.order.invoice.entities.Invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class InvoiceDtos {

    public record LineResponse(String description, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

    public record TaxResponse(String name, BigDecimal rate, BigDecimal amount) {}

    public record InvoiceResponse(
            String invoiceNumber, Long orderId, Instant issuedAt, String currency,
            String sellerName, String sellerAddress, String sellerGstin, String customerEmail,
            List<LineResponse> lines,
            BigDecimal taxableAmount, List<TaxResponse> taxes, BigDecimal totalTax, BigDecimal totalAmount
    ) {
        public static InvoiceResponse from(Invoice i) {
            return new InvoiceResponse(
                    i.getInvoiceNumber(), i.getOrderId(), i.getIssuedAt(), i.getCurrency(),
                    i.getSellerName(), i.getSellerAddress(), i.getSellerGstin(), i.getCustomerEmail(),
                    i.getLines().stream()
                            .map(l -> new LineResponse(l.getDescription(), l.getQuantity(), l.getUnitPrice(), l.getLineTotal()))
                            .toList(),
                    i.getTaxableAmount(),
                    i.getTaxes().stream()
                            .map(t -> new TaxResponse(t.getName(), t.getRate(), t.getAmount()))
                            .toList(),
                    i.getTotalTax(), i.getTotalAmount());
        }
    }

    public record PdfFile(String fileName, byte[] content) {}
}
