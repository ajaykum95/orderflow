package com.orderflow.order.report.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document(collection = "daily_sales_reports")
public class DailySalesReport {

    public static class TopProduct {
        private Long productId;
        private String name;
        private long quantity;
        private BigDecimal revenue;

        protected TopProduct() {}

        public TopProduct(Long productId, String name, long quantity, BigDecimal revenue) {
            this.productId = productId;
            this.name = name;
            this.quantity = quantity;
            this.revenue = revenue;
        }

        public Long getProductId() { return productId; }
        public String getName() { return name; }
        public long getQuantity() { return quantity; }
        public BigDecimal getRevenue() { return revenue; }
    }

    @Id
    private String date;                 // yyyy-MM-dd

    private Instant generatedAt;
    private long totalOrders;
    private long completedOrders;
    private long cancelledOrders;
    private long failedOrders;
    private long inProgressOrders;       // PENDING / STOCK_RESERVED / PAID
    private BigDecimal grossSales;
    private BigDecimal taxCollected;
    private BigDecimal averageOrderValue;
    private List<TopProduct> topProducts;

    protected DailySalesReport() {}

    public DailySalesReport(String date, Instant generatedAt, long totalOrders, long completedOrders,
                            long cancelledOrders, long failedOrders, long inProgressOrders,
                            BigDecimal grossSales, BigDecimal taxCollected, BigDecimal averageOrderValue,
                            List<TopProduct> topProducts) {
        this.date = date;
        this.generatedAt = generatedAt;
        this.totalOrders = totalOrders;
        this.completedOrders = completedOrders;
        this.cancelledOrders = cancelledOrders;
        this.failedOrders = failedOrders;
        this.inProgressOrders = inProgressOrders;
        this.grossSales = grossSales;
        this.taxCollected = taxCollected;
        this.averageOrderValue = averageOrderValue;
        this.topProducts = topProducts;
    }

    public String getDate() { return date; }
    public Instant getGeneratedAt() { return generatedAt; }
    public long getTotalOrders() { return totalOrders; }
    public long getCompletedOrders() { return completedOrders; }
    public long getCancelledOrders() { return cancelledOrders; }
    public long getFailedOrders() { return failedOrders; }
    public long getInProgressOrders() { return inProgressOrders; }
    public BigDecimal getGrossSales() { return grossSales; }
    public BigDecimal getTaxCollected() { return taxCollected; }
    public BigDecimal getAverageOrderValue() { return averageOrderValue; }
    public List<TopProduct> getTopProducts() { return topProducts; }
}
