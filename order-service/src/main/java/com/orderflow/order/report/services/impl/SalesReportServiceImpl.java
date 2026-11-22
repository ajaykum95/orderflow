package com.orderflow.order.report.services.impl;

import com.orderflow.order.order.entities.OrderStatus;
import com.orderflow.order.report.entities.DailySalesReport;
import com.orderflow.order.report.entities.DailySalesReport.TopProduct;
import com.orderflow.order.report.repositories.DailySalesReportRepository;
import com.orderflow.order.report.repositories.ReportOrderRepository;
import com.orderflow.order.report.services.SalesReportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesReportServiceImpl implements SalesReportService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final ReportOrderRepository orders;
    private final DailySalesReportRepository reports;

    public SalesReportServiceImpl(ReportOrderRepository orders, DailySalesReportRepository reports) {
        this.orders = orders;
        this.reports = reports;
    }

    @Override
    public DailySalesReport generate(LocalDate date) {
        // "दिन" भारतीय समय के हिसाब से: रात 12 से अगली रात 12 तक
        Instant from = date.atStartOfDay(IST).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(IST).toInstant();

        // हर status की गिनती और रकम
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        Map<OrderStatus, BigDecimal> sums = new EnumMap<>(OrderStatus.class);
        for (Object[] row : orders.statusSummary(from, to)) {
            OrderStatus status = (OrderStatus) row[0];
            counts.put(status, ((Number) row[1]).longValue());
            sums.put(status, row[2] == null ? ZERO : new BigDecimal(row[2].toString()));
        }

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        long completed = counts.getOrDefault(OrderStatus.COMPLETED, 0L);
        long cancelled = counts.getOrDefault(OrderStatus.CANCELLED, 0L);
        long failed = counts.getOrDefault(OrderStatus.FAILED, 0L);
        long inProgress = counts.getOrDefault(OrderStatus.PENDING, 0L)
                + counts.getOrDefault(OrderStatus.STOCK_RESERVED, 0L)
                + counts.getOrDefault(OrderStatus.PAID, 0L);

        BigDecimal gross = sums.getOrDefault(OrderStatus.COMPLETED, ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal average = completed == 0 ? ZERO
                : gross.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP);

        BigDecimal tax = orders.taxCollected(OrderStatus.COMPLETED, from, to);
        tax = tax == null ? ZERO : tax.setScale(2, RoundingMode.HALF_UP);

        List<TopProduct> top = new ArrayList<>();
        for (Object[] row : orders.topProducts(OrderStatus.COMPLETED, from, to, PageRequest.of(0, 5))) {
            top.add(new TopProduct((Long) row[0], (String) row[1], ((Number) row[2]).longValue(),
                    new BigDecimal(row[3].toString()).setScale(2, RoundingMode.HALF_UP)));
        }

        DailySalesReport report = new DailySalesReport(date.toString(), Instant.now(), total, completed,
                cancelled, failed, inProgress, gross, tax, average, top);
        return reports.save(report);   // _id तारीख है, इसलिए दोबारा बनाने पर पुरानी report बदल जाती है
    }

    @Override
    public DailySalesReport get(LocalDate date) {
        return reports.findById(date.toString())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "इस तारीख की report अभी बनी नहीं है (POST /api/admin/reports/daily?date=" + date + " से बनाएँ)"));
    }

    @Override
    public List<DailySalesReport> range(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from, to से बाद की नहीं हो सकती");
        }
        if (from.plusDays(92).isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "एक बार में ज़्यादा से ज़्यादा 93 दिन");
        }
        return reports.findByDateBetweenOrderByDateAsc(from.toString(), to.toString());
    }
}
