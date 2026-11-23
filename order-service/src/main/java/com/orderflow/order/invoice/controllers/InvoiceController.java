package com.orderflow.order.invoice.controllers;

import com.orderflow.order.common.ApiResponse;
import com.orderflow.order.invoice.dtos.InvoiceDtos.InvoiceResponse;
import com.orderflow.order.invoice.dtos.InvoiceDtos.PdfFile;
import com.orderflow.order.invoice.services.InvoiceService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/{orderId}/invoice")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> get(@PathVariable Long orderId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(200, "Invoice fetched",
                invoiceService.getByOrder(orderId, auth.getName(), isAdmin(auth))));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long orderId, Authentication auth) {
        PdfFile file = invoiceService.getPdf(orderId, auth.getName(), isAdmin(auth));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.fileName()).build().toString())
                .body(file.content());
    }

    private static boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
