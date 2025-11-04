package com.example.demo.controller;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.ReportService;

@RestController
@RequestMapping("/internal")
public class ReportController {

    private final ReportService service;

    @Value("${report.internal.token}")
    private String internalToken;

    public ReportController(ReportService service) { this.service = service; }

    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> generate(
            @RequestHeader(name = "X-Internal-Token", required = false) String token) {

        if (token == null || !token.equals(internalToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "forbidden"));
        }

        Map<String, Object> stats = service.buildStats(); // Executa SQL no RDS
        return ResponseEntity.ok(Map.of(
                "generatedAt", OffsetDateTime.now().toString(),
                "stats", stats
        ));
    }
}
