package com.orderflow.order.invoice.services;

import com.orderflow.order.invoice.config.TaxRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

// टैक्स का हिसाब। इसमें Spring या database कुछ नहीं, सिर्फ़ गणित; इसलिए test करना आसान है।
//
// हमारे दाम "टैक्स समेत" (inclusive) हैं, जैसे दुकानों में MRP होता है।
// यानी customer ने जो रकम दी, वही invoice का कुल है; invoice सिर्फ़ यह बताता है कि उसमें टैक्स कितना है।
//
//   कुल (gross) = 1000, GST 18%
//   टैक्स से पहले की रकम = 1000 / 1.18 = 847.46
//   GST                  = 847.46 x 18% = 152.54
public final class TaxCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private TaxCalculator() {}

    public record TaxLine(String name, BigDecimal rate, BigDecimal amount) {}

    public record Breakdown(BigDecimal taxableAmount, List<TaxLine> taxes, BigDecimal totalTax, BigDecimal total) {}

    public static Breakdown calculateInclusive(BigDecimal gross, List<TaxRule> rules) {
        BigDecimal total = gross.setScale(2, RoundingMode.HALF_UP);

        if (rules == null || rules.isEmpty()) {
            return new Breakdown(total, List.of(), BigDecimal.ZERO.setScale(2), total);
        }

        BigDecimal totalRate = BigDecimal.ZERO;
        for (TaxRule r : rules) {
            totalRate = totalRate.add(r.getRate());
        }

        // टैक्स से पहले की रकम
        BigDecimal divisor = BigDecimal.ONE.add(totalRate.divide(HUNDRED, 10, RoundingMode.HALF_UP));
        BigDecimal base = total.divide(divisor, 2, RoundingMode.HALF_UP);

        // हर नियम का टैक्स
        List<TaxLine> lines = new ArrayList<>();
        BigDecimal taxSum = BigDecimal.ZERO;
        for (TaxRule r : rules) {
            BigDecimal amount = base.multiply(r.getRate()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            lines.add(new TaxLine(r.getName(), r.getRate(), amount));
            taxSum = taxSum.add(amount);
        }

        // पैसे में गोल करने से 1-2 पैसे का फ़र्क आ सकता है। उसे आख़िरी टैक्स-लाइन में जोड़ देते हैं,
        // ताकि हमेशा  (टैक्स से पहले की रकम + टैक्स) == कुल  सटीक रहे।
        BigDecimal diff = total.subtract(base).subtract(taxSum);
        if (diff.signum() != 0) {
            int last = lines.size() - 1;
            TaxLine l = lines.get(last);
            lines.set(last, new TaxLine(l.name(), l.rate(), l.amount().add(diff)));
            taxSum = taxSum.add(diff);
        }

        return new Breakdown(base, lines, taxSum, total);
    }
}
