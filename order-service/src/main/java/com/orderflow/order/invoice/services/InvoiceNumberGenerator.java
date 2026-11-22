package com.orderflow.order.invoice.services;

import com.orderflow.order.invoice.entities.InvoiceCounter;
import com.orderflow.order.invoice.repositories.InvoiceCounterRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// क्रमवार (बिना खाली जगह के) invoice नंबर:  INV-2026-27-000001, 000002, 000003 ...
//
// Postgres का सामान्य "sequence" यहाँ ठीक नहीं: transaction लौटे तो भी sequence आगे बढ़ चुका होता है,
// और नंबर छूट जाते हैं। भारत में GST invoice के नंबर लगातार होने चाहिए।
//
// इसलिए हम counter की पंक्ति पर ताला लगाकर नंबर बढ़ाते हैं, और यह invoice के save वाले
// उसी transaction में होता है: invoice फेल हुआ तो नंबर भी वापस लौट जाता है। कोई नंबर नहीं छूटता।
@Component
public class InvoiceNumberGenerator {

    private final InvoiceCounterRepository counters;

    public InvoiceNumberGenerator(InvoiceCounterRepository counters) {
        this.counters = counters;
    }

    // MANDATORY = पहले से चल रहे transaction के अंदर ही बुलाया जा सकता है
    @Transactional(propagation = Propagation.MANDATORY)
    public String next(LocalDate issueDate) {
        String fy = financialYear(issueDate);
        counters.createIfMissing(fy);
        InvoiceCounter counter = counters.lockByFinancialYear(fy).orElseThrow();
        return String.format("INV-%s-%06d", fy, counter.next());
    }

    // भारत का वित्त-वर्ष अप्रैल से मार्च: 15 नवंबर 2026 -> "2026-27", 10 फ़रवरी 2027 -> "2026-27"
    static String financialYear(LocalDate d) {
        int startYear = d.getMonthValue() >= 4 ? d.getYear() : d.getYear() - 1;
        return startYear + "-" + String.format("%02d", (startYear + 1) % 100);
    }
}
