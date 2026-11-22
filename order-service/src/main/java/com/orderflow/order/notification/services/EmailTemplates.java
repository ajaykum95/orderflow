package com.orderflow.order.notification.services;

import com.orderflow.order.invoice.dtos.InvoiceDtos.InvoiceResponse;
import com.orderflow.order.order.entities.Order;
import com.orderflow.order.order.entities.OrderItem;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.util.Locale;

// Email का मज़मून। अभी Java में लिखा है; आगे चाहें तो Thymeleaf template में बदल सकते हैं।
public final class EmailTemplates {

    private EmailTemplates() {}

    public record Content(String subject, String html) {}

    public static Content orderConfirmed(String storeName, Order order, InvoiceResponse invoice) {
        StringBuilder h = new StringBuilder();
        h.append("<div style=\"font-family:Arial,sans-serif;max-width:560px\">");
        h.append("<h2>Thank you for your order!</h2>");
        h.append("<p>Your order <b>#").append(order.getId()).append("</b> is confirmed.</p>");

        h.append("<table style=\"width:100%;border-collapse:collapse\">");
        for (OrderItem i : order.getItems()) {
            h.append("<tr><td style=\"padding:6px;border-bottom:1px solid #eee\">")
                    .append(HtmlUtils.htmlEscape(i.getProductName()))
                    .append(" &times; ").append(i.getQuantity())
                    .append("</td><td style=\"padding:6px;border-bottom:1px solid #eee;text-align:right\">")
                    .append(money(i.lineTotal())).append("</td></tr>");
        }
        h.append("<tr><td style=\"padding:6px\"><b>Total</b></td>")
                .append("<td style=\"padding:6px;text-align:right\"><b>")
                .append(money(order.getTotalAmount())).append("</b></td></tr>");
        h.append("</table>");

        // invoice का हिस्सा सिर्फ़ तब, जब invoice बना हो (workflow में INVOICE step हट भी सकता है)
        if (invoice != null) {
            h.append("<p>Invoice no: <b>").append(HtmlUtils.htmlEscape(invoice.invoiceNumber())).append("</b><br>");
            h.append("Taxable value: ").append(money(invoice.taxableAmount())).append("<br>");
            invoice.taxes().forEach(t -> h.append(HtmlUtils.htmlEscape(t.name())).append(": ")
                    .append(money(t.amount())).append("<br>"));
            h.append("</p><p>Your invoice is attached as a PDF.</p>");
        }

        h.append("<p style=\"color:#888\">").append(HtmlUtils.htmlEscape(storeName)).append("</p></div>");

        String subject = invoice != null
                ? "Order #" + order.getId() + " confirmed - Invoice " + invoice.invoiceNumber()
                : "Order #" + order.getId() + " confirmed";
        return new Content(subject, h.toString());
    }

    // Customer को अंदर की तकनीकी वजह (जैसे "circuit breaker open") नहीं बताते; सिर्फ़ सीधी, सच्ची बात
    public static Content orderCancelled(String storeName, Order order) {
        String html = "<div style=\"font-family:Arial,sans-serif;max-width:560px\">"
                + "<h2>We could not complete your order</h2>"
                + "<p>Sorry, your order <b>#" + order.getId() + "</b> could not be completed.</p>"
                + "<p>If any amount was charged, it has been refunded. "
                + "Items held for you have been released.</p>"
                + "<p>You are welcome to try again.</p>"
                + "<p style=\"color:#888\">" + HtmlUtils.htmlEscape(storeName) + "</p></div>";
        return new Content("Order #" + order.getId() + " could not be completed", html);
    }

    private static String money(BigDecimal v) {
        return String.format(Locale.ENGLISH, "INR %,.2f", v);
    }
}
