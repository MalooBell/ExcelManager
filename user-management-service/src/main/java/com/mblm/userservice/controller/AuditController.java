package com.mblm.userservice.controller;

import com.mblm.userservice.dto.AuditRequest;
import com.mblm.userservice.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/log")
    public ResponseEntity<Void> logAction(@RequestBody AuditRequest request) {
        auditService.logAction(request);
        return ResponseEntity.ok().build();
    }
}