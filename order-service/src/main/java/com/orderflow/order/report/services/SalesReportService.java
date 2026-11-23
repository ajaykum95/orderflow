package com.orderflow.order.report.services;

import com.orderflow.order.report.entities.DailySalesReport;

import java.time.LocalDate;
import java.util.List;

public interface SalesReportService {

    DailySalesReport generate(LocalDate date);

    DailySalesReport get(LocalDate date);

    List<DailySalesReport> range(LocalDate from, LocalDate to);
}
