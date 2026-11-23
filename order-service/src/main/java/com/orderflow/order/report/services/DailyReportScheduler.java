package com.orderflow.order.report.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class DailyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyReportScheduler.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final SalesReportService reportService;

    public DailyReportScheduler(SalesReportService reportService) {
        this.reportService = reportService;
    }

    @Scheduled(cron = "${app.report.daily-cron:0 5 0 * * *}", zone = "Asia/Kolkata")
    public void generateYesterday() {
        LocalDate yesterday = LocalDate.now(IST).minusDays(1);
        try {
            var report = reportService.generate(yesterday);
            log.info("Daily report created: {} -> {} total orders {}", yesterday,
                    report.getTotalOrders(), report.getCompletedOrders());
        } catch (Exception e) {
            log.error("Daily report ({}) not generated", yesterday, e);
        }
    }
}
