package com.orderflow.order.report.repositories;

import com.orderflow.order.report.entities.DailySalesReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DailySalesReportRepository extends MongoRepository<DailySalesReport, String> {

    // _id "yyyy-MM-dd" है, और ऐसी तारीखें अक्षरों के क्रम में भी सही क्रम में रहती हैं
    List<DailySalesReport> findByDateBetweenOrderByDateAsc(String from, String to);
}
