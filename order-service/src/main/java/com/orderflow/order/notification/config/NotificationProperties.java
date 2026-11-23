package com.orderflow.order.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    private String from = "no-reply@orderflow.local";

    private boolean enabled = true;

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
