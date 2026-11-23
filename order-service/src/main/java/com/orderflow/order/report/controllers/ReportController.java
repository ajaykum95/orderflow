package com.orderflow.order.report.controllers;

import com.orderflow.order.common.ApiResponse;
import com.orderflow.order.report.entities.DailySalesReport;
import com.orderflow.order.report.services.SalesReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reports/daily")
public class ReportController {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final SalesReportService reportService;

    public ReportController(SalesReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DailySalesReport>> generate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now(IST);
        return ResponseEntity.ok(ApiResponse.ok(200, "Report generated for " + d, reportService.generate(d)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DailySalesReport>> get(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(200, "Report fetched", reportService.get(date)));
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<DailySalesReport>>> range(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(200, "Reports fetched", reportService.range(from, to)));
    }
}
