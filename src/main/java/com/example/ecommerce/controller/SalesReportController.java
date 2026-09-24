package com.example.ecommerce.controller;


import com.example.ecommerce.dto.SalesReportResponse;
import com.example.ecommerce.service.SalesReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sales")
public class SalesReportController {

    private final SalesReportService salesReportService;

    public SalesReportController(
            SalesReportService salesReportService) {

        this.salesReportService =
                salesReportService;
    }


    @GetMapping("/report")
    public ResponseEntity<SalesReportResponse>
    getSalesReport() {

        return ResponseEntity.ok(
                salesReportService.getSalesReport()
        );
    }
}