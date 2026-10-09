package com.examly.springapp.service;

public interface InvoiceService {

    byte[] generateInvoicePdf(
            Long orderId)
            throws Exception;
} 