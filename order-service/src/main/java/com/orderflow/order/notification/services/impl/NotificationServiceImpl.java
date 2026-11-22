package com.orderflow.order.notification.services.impl;

import com.orderflow.order.invoice.config.InvoiceProperties;
import com.orderflow.order.invoice.dtos.InvoiceDtos.InvoiceResponse;
import com.orderflow.order.invoice.dtos.InvoiceDtos.PdfFile;
import com.orderflow.order.invoice.services.InvoiceService;
import com.orderflow.order.notification.config.NotificationProperties;
import com.orderflow.order.notification.entities.NotificationLog;
import com.orderflow.order.notification.entities.NotificationType;
import com.orderflow.order.notification.repositories.NotificationLogRepository;
import com.orderflow.order.notification.services.EmailTemplates;
import com.orderflow.order.notification.services.EmailTemplates.Content;
import com.orderflow.order.notification.services.NotificationService;
import com.orderflow.order.order.entities.Order;
import com.orderflow.order.order.entities.OrderStatus;
import com.orderflow.order.order.repositories.OrderRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final JavaMailSender mailSender;
    private final OrderRepository orders;
    private final InvoiceService invoices;
    private final NotificationLogRepository logs;
    private final NotificationProperties props;
    private final InvoiceProperties invoiceProps;   // दुकान का नाम यहीं से

    public NotificationServiceImpl(JavaMailSender mailSender, OrderRepository orders, InvoiceService invoices,
                                   NotificationLogRepository logs, NotificationProperties props,
                                   InvoiceProperties invoiceProps) {
        this.mailSender = mailSender;
        this.orders = orders;
        this.invoices = invoices;
        this.logs = logs;
        this.props = props;
        this.invoiceProps = invoiceProps;
    }

    @Override
    public void sendOrderConfirmation(Long orderId) {
        if (!props.isEnabled()) { log.info("[order {}] notifications बंद हैं", orderId); return; }

        // पहले ही भेज चुके हैं? तो कुछ मत करो (Temporal ने activity दोबारा चलाई होगी)
        if (logs.existsByOrderIdAndType(orderId, NotificationType.ORDER_CONFIRMED)) {
            log.info("[order {}] confirmation email पहले ही जा चुकी है", orderId);
            return;
        }

        Order order = load(orderId);
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.COMPLETED) {
            // रद्द order को "confirmed" की email कभी नहीं जानी चाहिए
            throw new IllegalStateException("Confirmation सिर्फ़ paid order की जाती है; order अभी " + order.getStatus() + " है");
        }

        // invoice हो तो उसे और उसका PDF जोड़ो; न हो (step हटा हो) तो बिना invoice के भेज दो
        InvoiceResponse invoice = null;
        PdfFile pdf = null;
        try {
            invoice = invoices.getByOrder(orderId, order.getCustomerEmail(), false);
            pdf = invoices.getPdf(orderId, order.getCustomerEmail(), false);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() != 404) throw e;
            invoice = null;
            pdf = null;
        }

        Content content = EmailTemplates.orderConfirmed(invoiceProps.getSellerName(), order, invoice);
        send(order.getCustomerEmail(), content, pdf);
        record(orderId, NotificationType.ORDER_CONFIRMED, order.getCustomerEmail());
    }

    @Override
    public void sendOrderCancellation(Long orderId) {
        if (!props.isEnabled()) { log.info("[order {}] notifications बंद हैं", orderId); return; }

        if (logs.existsByOrderIdAndType(orderId, NotificationType.ORDER_CANCELLED)) {
            log.info("[order {}] cancellation email पहले ही जा चुकी है", orderId);
            return;
        }

        Order order = load(orderId);
        if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.FAILED) {
            log.warn("[order {}] order {} है, cancellation email नहीं भेज रहे", orderId, order.getStatus());
            return;
        }

        send(order.getCustomerEmail(),
                EmailTemplates.orderCancelled(invoiceProps.getSellerName(), order), null);
        record(orderId, NotificationType.ORDER_CANCELLED, order.getCustomerEmail());
    }

    // MailSendException (SMTP बंद/धीमा) जानबूझकर नहीं पकड़ी: वह सामान्य exception है, Temporal retry करेगा
    private void send(String to, Content content, PdfFile pdf) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, pdf != null, "UTF-8");
            helper.setFrom(props.getFrom());
            helper.setTo(to);
            helper.setSubject(content.subject());
            helper.setText(content.html(), true);
            if (pdf != null) {
                helper.addAttachment(pdf.fileName(), new ByteArrayResource(pdf.content()), "application/pdf");
            }
            mailSender.send(message);
            log.info("Email भेजी: to={}, subject={}", to, content.subject());
        } catch (MessagingException e) {
            throw new IllegalStateException("Email बनाने में गड़बड़ी", e);
        }
    }

    private void record(Long orderId, NotificationType type, String recipient) {
        try {
            logs.save(new NotificationLog(orderId, type, recipient));
        } catch (DataIntegrityViolationException e) {
            // दो activity एक साथ चलीं और दूसरे ने पहले ही लिख दिया; कोई बात नहीं
            log.info("[order {}] {} का रिकॉर्ड पहले से है", orderId, type);
        }
    }

    private Order load(Long orderId) {
        return orders.findWithItemsById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));
    }
}
