package com.orderflow.order.invoice;

import com.orderflow.order.invoice.config.TaxRule;
import com.orderflow.order.invoice.services.TaxCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaxCalculatorTest {

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    @Test
    void singleGst18() {
        var b = TaxCalculator.calculateInclusive(bd("1000"), List.of(new TaxRule("GST", bd("18"))));
        assertEquals(bd("847.46"), b.taxableAmount());
        assertEquals(bd("152.54"), b.totalTax());
        assertEquals(bd("1000.00"), b.total());
    }

    @Test
    void cgstPlusSgstSplitsEvenly() {
        var b = TaxCalculator.calculateInclusive(bd("1000"),
                List.of(new TaxRule("CGST", bd("9")), new TaxRule("SGST", bd("9"))));
        assertEquals(2, b.taxes().size());
        assertEquals(bd("76.27"), b.taxes().get(0).amount());
        assertEquals(bd("76.27"), b.taxes().get(1).amount());
    }

    @Test
    void baseAndTaxAlwaysAddUpToTotal() {
        // कई अजीब रकमों पर जाँच: जोड़ हमेशा कुल के बराबर होना चाहिए, एक पैसा भी इधर-उधर नहीं
        for (String g : new String[]{"0.01", "1", "99.99", "333.33", "1234.56", "9999.99", "100000"}) {
            var b = TaxCalculator.calculateInclusive(bd(g),
                    List.of(new TaxRule("CGST", bd("9")), new TaxRule("SGST", bd("9"))));
            assertEquals(b.total(), b.taxableAmount().add(b.totalTax()), "रकम " + g);
        }
    }

    @Test
    void noTaxRulesMeansNoTax() {
        var b = TaxCalculator.calculateInclusive(bd("500"), List.of());
        assertEquals(bd("500.00"), b.taxableAmount());
        assertEquals(bd("0.00"), b.totalTax());
    }
}
