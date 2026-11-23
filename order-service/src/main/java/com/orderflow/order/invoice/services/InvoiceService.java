package com.orderflow.order.invoice.services;

import com.orderflow.order.invoice.dtos.InvoiceDtos.InvoiceResponse;
import com.orderflow.order.invoice.dtos.InvoiceDtos.PdfFile;

public interface InvoiceService {

    InvoiceResponse generateForOrder(Long orderId);

    InvoiceResponse getByOrder(Long orderId, String requesterEmail, boolean isAdmin);

    PdfFile getPdf(Long orderId, String requesterEmail, boolean isAdmin);
}
