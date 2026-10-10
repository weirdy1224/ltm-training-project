package com.examly.springapp.controller;

import com.examly.springapp.service.InvoiceService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoice")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(
            InvoiceService invoiceService) { 

        this.invoiceService =
                invoiceService;
    }

    @GetMapping("/order/{orderId}/pdf")
    public ResponseEntity<byte[]> downloadInvoice(
            @PathVariable Long orderId)
            throws Exception {
    
        System.out.println("Invoice API called. Order Id = " + orderId);
    
        byte[] pdf =
                invoiceService.generateInvoicePdf(orderId);
    
        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=invoice.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}