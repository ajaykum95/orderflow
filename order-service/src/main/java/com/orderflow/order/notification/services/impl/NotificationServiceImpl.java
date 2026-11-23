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
    private final InvoiceProperties invoiceProps;

    public NotificationServiceImpl(
            JavaMailSender mailSender, OrderRepository orders, InvoiceService invoices,
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
        if (!props.isEnabled()) { log.info("[order {}] notifications is closed", orderId); return; }

        if (logs.existsByOrderIdAndType(orderId, NotificationType.ORDER_CONFIRMED)) {
            log.info("[order {}] Confirmation email has already been sent", orderId);
            return;
        }

        Order order = load(orderId);
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Confirmation is only sent for paid orders; order status is currently " + order.getStatus());
        }

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
        if (!props.isEnabled()) { log.info("[order {}] notifications is closed.", orderId); return; }

        if (logs.existsByOrderIdAndType(orderId, NotificationType.ORDER_CANCELLED)) {
            log.info("[order {}] Cancellation email has already been sent", orderId);
            return;
        }

        Order order = load(orderId);
        if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.FAILED) {
            log.warn("[order {}] Order is {}, skipping cancellation email", orderId, order.getStatus());
            return;
        }

        send(order.getCustomerEmail(),
                EmailTemplates.orderCancelled(invoiceProps.getSellerName(), order), null);
        record(orderId, NotificationType.ORDER_CANCELLED, order.getCustomerEmail());
    }

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
            log.info("Email Sent: to={}, subject={}", to, content.subject());
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to create email", e);
        }
    }

    private void record(Long orderId, NotificationType type, String recipient) {
        try {
            logs.save(new NotificationLog(orderId, type, recipient));
        } catch (DataIntegrityViolationException e) {
            log.info("[order {}] Record for {} already exists", orderId, type);
        }
    }

    private Order load(Long orderId) {
        return orders.findWithItemsById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));
    }
}
