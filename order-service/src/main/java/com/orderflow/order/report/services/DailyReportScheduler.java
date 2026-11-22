package com.orderflow.order.report.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

// हर रात (भारतीय समय) बीते कल की report अपने-आप बनाता है।
// Cron application.yml के app.report.daily-cron से बदला जा सकता है।
// दो instance चलें तो दोनों एक ही document लिखेंगे (तारीख = _id), इसलिए कोई दोहराव नहीं।
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
            log.info("Daily report बनी: {} -> {} orders, {} पूरे", yesterday,
                    report.getTotalOrders(), report.getCompletedOrders());
        } catch (Exception e) {
            log.error("Daily report ({}) नहीं बन सकी", yesterday, e);   // scheduler बंद न हो, अगली रात फिर चलेगा
        }
    }
}
