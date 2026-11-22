package com.orderflow.order.report.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// @Scheduled वाले काम चलाने के लिए यह चालू करना पड़ता है
@Configuration
@EnableScheduling
public class ReportConfig {
}
