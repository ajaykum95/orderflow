package com.orderflow.order.invoice.services.impl;

import com.orderflow.order.invoice.config.InvoiceProperties;
import com.orderflow.order.invoice.dtos.InvoiceDtos.InvoiceResponse;
import com.orderflow.order.invoice.dtos.InvoiceDtos.PdfFile;
import com.orderflow.order.invoice.entities.Invoice;
import com.orderflow.order.invoice.entities.InvoiceLine;
import com.orderflow.order.invoice.entities.InvoiceTax;
import com.orderflow.order.invoice.pdf.InvoicePdfGenerator;
import com.orderflow.order.invoice.repositories.InvoiceRepository;
import com.orderflow.order.invoice.services.InvoiceNumberGenerator;
import com.orderflow.order.invoice.services.InvoiceService;
import com.orderflow.order.invoice.services.TaxCalculator;
import com.orderflow.order.order.entities.Order;
import com.orderflow.order.order.entities.OrderItem;
import com.orderflow.order.order.entities.OrderStatus;
import com.orderflow.order.order.repositories.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final InvoiceRepository invoices;
    private final OrderRepository orders;
    private final InvoiceNumberGenerator numbers;
    private final InvoicePdfGenerator pdfGenerator;
    private final InvoiceProperties props;

    public InvoiceServiceImpl(InvoiceRepository invoices, OrderRepository orders,
                              InvoiceNumberGenerator numbers, InvoicePdfGenerator pdfGenerator,
                              InvoiceProperties props) {
        this.invoices = invoices;
        this.orders = orders;
        this.numbers = numbers;
        this.pdfGenerator = pdfGenerator;
        this.props = props;
    }

    // नंबर लेना और invoice save करना एक ही transaction में; कुछ फेल हुआ तो नंबर भी वापस
    @Override
    @Transactional
    public InvoiceResponse generateForOrder(Long orderId) {

        // पहले से बना है? (Temporal ने activity दोबारा चलाई हो तो) -> वही लौटा दो, नया नंबर मत खर्चो
        var existing = invoices.findByOrderId(orderId);
        if (existing.isPresent()) {
            return InvoiceResponse.from(existing.get());
        }

        Order order = orders.findWithItemsById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Invoice सिर्फ़ paid order का बनता है; order अभी " + order.getStatus() + " है");
        }

        Instant now = Instant.now();
        var tax = TaxCalculator.calculateInclusive(order.getTotalAmount(), props.getTaxRules());
        String number = numbers.next(LocalDate.ofInstant(now, IST));

        Invoice invoice = new Invoice(number, order.getId(), order.getCustomerEmail(), props.getCurrency(),
                props.getSellerName(), props.getSellerAddress(), props.getSellerGstin(),
                tax.taxableAmount(), tax.totalTax(), tax.total(), now);

        for (OrderItem item : order.getItems()) {
            invoice.addLine(new InvoiceLine(item.getProductName(), item.getQuantity(),
                    item.getUnitPrice(), item.lineTotal()));
        }
        for (var t : tax.taxes()) {
            invoice.addTax(new InvoiceTax(t.name(), t.rate(), t.amount()));
        }

        return InvoiceResponse.from(invoices.save(invoice));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getByOrder(Long orderId, String requesterEmail, boolean isAdmin) {
        return InvoiceResponse.from(findAuthorized(orderId, requesterEmail, isAdmin));
    }

    // PDF हर बार invoice के डेटा से बनता है; डेटा बदलता नहीं, इसलिए PDF हमेशा एक जैसा
    @Override
    @Transactional(readOnly = true)
    public PdfFile getPdf(Long orderId, String requesterEmail, boolean isAdmin) {
        Invoice invoice = findAuthorized(orderId, requesterEmail, isAdmin);
        return new PdfFile("invoice-" + invoice.getInvoiceNumber() + ".pdf", pdfGenerator.generate(invoice));
    }

    private Invoice findAuthorized(Long orderId, String email, boolean isAdmin) {
        return invoices.findByOrderId(orderId)
                .filter(i -> isAdmin || i.getCustomerEmail().equals(email))
                // दूसरे का हो तब भी 404, ताकि पता न चले कि मौजूद है
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Invoice not found (order अभी पूरा नहीं हुआ हो सकता है)"));
    }
}
