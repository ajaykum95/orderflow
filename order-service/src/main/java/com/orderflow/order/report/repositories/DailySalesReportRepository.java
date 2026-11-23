package com.orderflow.order.report.repositories;

import com.orderflow.order.report.entities.DailySalesReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DailySalesReportRepository extends MongoRepository<DailySalesReport, String> {

    List<DailySalesReport> findByDateBetweenOrderByDateAsc(String from, String to);
}
