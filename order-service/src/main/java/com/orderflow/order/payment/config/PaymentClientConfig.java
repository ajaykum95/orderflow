package com.orderflow.order.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class PaymentClientConfig {

    @Bean
    public RestClient paymentRestClient(
            RestClient.Builder builder, @Value("${app.payment.base-url}") String baseUrl,
            @Value("${app.payment.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${app.payment.read-timeout-ms}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        return builder.baseUrl(baseUrl).requestFactory(factory).build();
    }
}
