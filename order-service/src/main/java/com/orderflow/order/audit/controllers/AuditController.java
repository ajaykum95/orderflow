package com.orderflow.order.audit.controllers;

import com.orderflow.order.audit.dtos.AuditDtos.AuditEventResponse;
import com.orderflow.order.audit.services.AuditService;
import com.orderflow.order.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<List<AuditEventResponse>>> timeline(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.ok(200, "Audit timeline", auditService.timeline(orderId)));
    }
}
