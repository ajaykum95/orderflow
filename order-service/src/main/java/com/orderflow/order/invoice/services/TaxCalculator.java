package com.orderflow.order.invoice.services;

import com.orderflow.order.invoice.config.TaxRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

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

        BigDecimal divisor = BigDecimal.ONE.add(totalRate.divide(HUNDRED, 10, RoundingMode.HALF_UP));
        BigDecimal base = total.divide(divisor, 2, RoundingMode.HALF_UP);

        List<TaxLine> lines = new ArrayList<>();
        BigDecimal taxSum = BigDecimal.ZERO;
        for (TaxRule r : rules) {
            BigDecimal amount = base.multiply(r.getRate()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            lines.add(new TaxLine(r.getName(), r.getRate(), amount));
            taxSum = taxSum.add(amount);
        }

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
