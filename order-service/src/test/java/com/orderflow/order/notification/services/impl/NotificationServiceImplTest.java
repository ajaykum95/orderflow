package com.orderflow.order.notification.services.impl;

import com.orderflow.order.invoice.config.InvoiceProperties;
import com.orderflow.order.invoice.dtos.InvoiceDtos.InvoiceResponse;
import com.orderflow.order.invoice.dtos.InvoiceDtos.LineResponse;
import com.orderflow.order.invoice.dtos.InvoiceDtos.PdfFile;
import com.orderflow.order.invoice.dtos.InvoiceDtos.TaxResponse;
import com.orderflow.order.invoice.services.InvoiceService;
import com.orderflow.order.notification.config.NotificationProperties;
import com.orderflow.order.notification.entities.NotificationLog;
import com.orderflow.order.notification.entities.NotificationType;
import com.orderflow.order.notification.repositories.NotificationLogRepository;
import com.orderflow.order.notification.services.EmailTemplates;
import com.orderflow.order.order.entities.Order;
import com.orderflow.order.order.entities.OrderItem;
import com.orderflow.order.order.entities.OrderStatus;
import com.orderflow.order.order.repositories.OrderRepository;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final Long ID = 1L;
    private static final String CUSTOMER = "cust@test.com";

    @Mock JavaMailSender mailSender;
    @Mock OrderRepository orders;
    @Mock InvoiceService invoices;
    @Mock NotificationLogRepository logs;

    private final NotificationProperties props = new NotificationProperties();
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(mailSender, orders, invoices, logs, props, new InvoiceProperties());
    }

    private static Order orderIn(OrderStatus target) {
        Order o = new Order(CUSTOMER, "k1");
        o.addItem(new OrderItem(1L, "Phone", new BigDecimal("500.00"), 2));
        switch (target) {
            case PAID -> { o.moveTo(OrderStatus.STOCK_RESERVED); o.moveTo(OrderStatus.PAID); }
            case CANCELLED -> o.moveTo(OrderStatus.CANCELLED);
            case FAILED -> o.moveTo(OrderStatus.FAILED);
            case COMPLETED -> {
                o.moveTo(OrderStatus.STOCK_RESERVED); o.moveTo(OrderStatus.PAID); o.moveTo(OrderStatus.COMPLETED);
            }
            default -> { /* PENDING */ }
        }
        return o;
    }

    private static MimeMessage newMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private static InvoiceResponse sampleInvoice() {
        return new InvoiceResponse("INV-2026-27-000001", ID, Instant.now(), "INR",
                "Store", "Addr", "GSTIN", CUSTOMER,
                List.of(new LineResponse("Phone", 2, new BigDecimal("500.00"), new BigDecimal("1000.00"))),
                new BigDecimal("847.46"),
                List.of(new TaxResponse("GST", new BigDecimal("18"), new BigDecimal("152.54"))),
                new BigDecimal("152.54"), new BigDecimal("1000.00"));
    }

    @Test
    void confirmationIsSentWithInvoicePdfAndRecorded() throws Exception {
        when(logs.existsByOrderIdAndType(ID, NotificationType.ORDER_CONFIRMED)).thenReturn(false);
        when(orders.findWithItemsById(ID)).thenReturn(Optional.of(orderIn(OrderStatus.PAID)));
        when(invoices.getByOrder(ID, CUSTOMER, false)).thenReturn(sampleInvoice());
        when(invoices.getPdf(ID, CUSTOMER, false)).thenReturn(new PdfFile("invoice.pdf", new byte[]{1, 2, 3}));
        when(mailSender.createMimeMessage()).thenReturn(newMessage());

        service.sendOrderConfirmation(ID);

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().getSubject()).contains("INV-2026-27-000001");
        assertThat(sent.getValue().getContent()).isInstanceOf(Multipart.class);
        verify(logs).save(any(NotificationLog.class));
    }

    @Test
    void confirmationIsNotSentTwice() {
        when(logs.existsByOrderIdAndType(ID, NotificationType.ORDER_CONFIRMED)).thenReturn(true);

        service.sendOrderConfirmation(ID);

        verifyNoInteractions(mailSender, orders, invoices);
        verify(logs, never()).save(any());
    }

    @Test
    void confirmationForUnpaidOrderIsRefused() {
        when(logs.existsByOrderIdAndType(ID, NotificationType.ORDER_CONFIRMED)).thenReturn(false);
        when(orders.findWithItemsById(ID)).thenReturn(Optional.of(orderIn(OrderStatus.CANCELLED)));

        assertThatThrownBy(() -> service.sendOrderConfirmation(ID)).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(mailSender);
    }

    @Test
    void confirmationStillGoesOutWhenThereIsNoInvoice() throws Exception {
        when(logs.existsByOrderIdAndType(ID, NotificationType.ORDER_CONFIRMED)).thenReturn(false);
        when(orders.findWithItemsById(ID)).thenReturn(Optional.of(orderIn(OrderStatus.PAID)));
        when(invoices.getByOrder(ID, CUSTOMER, false))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "no invoice"));
        when(mailSender.createMimeMessage()).thenReturn(newMessage());

        service.sendOrderConfirmation(ID);

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().getSubject()).contains("confirmed").doesNotContain("Invoice");
        verify(invoices, never()).getPdf(any(), any(), anyBoolean());
    }

    @Test
    void cancellationIsSentForCancelledAndFailedOrders() {
        for (OrderStatus status : new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.FAILED}) {
            reset(mailSender, orders, logs);
            when(logs.existsByOrderIdAndType(ID, NotificationType.ORDER_CANCELLED)).thenReturn(false);
            when(orders.findWithItemsById(ID)).thenReturn(Optional.of(orderIn(status)));
            when(mailSender.createMimeMessage()).thenReturn(newMessage());

            service.sendOrderCancellation(ID);

            verify(mailSender).send(any(MimeMessage.class));
            verify(logs).save(any(NotificationLog.class));
        }
    }

    @Test
    void cancellationIsNotSentForCompletedOrder() {
        when(logs.existsByOrderIdAndType(ID, NotificationType.ORDER_CANCELLED)).thenReturn(false);
        when(orders.findWithItemsById(ID)).thenReturn(Optional.of(orderIn(OrderStatus.COMPLETED)));

        service.sendOrderCancellation(ID);

        verifyNoInteractions(mailSender);
        verify(logs, never()).save(any());
    }

    @Test
    void nothingHappensWhenNotificationsAreDisabled() {
        props.setEnabled(false);

        service.sendOrderConfirmation(ID);
        service.sendOrderCancellation(ID);

        verifyNoInteractions(mailSender, orders, invoices, logs);
    }

    @Test
    void productNamesAreHtmlEscaped() {
        Order o = new Order(CUSTOMER, "k1");
        o.addItem(new OrderItem(1L, "<script>alert(1)</script>", new BigDecimal("10.00"), 1));

        String html = EmailTemplates.orderConfirmed("Store", o, null).html();

        assertThat(html).doesNotContain("<script>").contains("&lt;script&gt;");
    }
}
