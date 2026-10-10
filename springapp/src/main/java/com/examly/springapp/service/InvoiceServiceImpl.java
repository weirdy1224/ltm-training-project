// package com.examly.springapp.service;

// import java.io.ByteArrayOutputStream;

// import org.springframework.stereotype.Service;

// import com.examly.springapp.model.Course;
// import com.examly.springapp.model.Orders;
// import com.examly.springapp.repository.OrderRepo;
// import com.examly.springapp.service.InvoiceService;

// import com.itextpdf.kernel.pdf.PdfDocument; 
// import com.itextpdf.kernel.pdf.PdfWriter;
// import com.itextpdf.layout.Document;
// import com.itextpdf.layout.element.Paragraph;
// import com.itextpdf.layout.element.Table;

// @Service
// public class InvoiceServiceImpl
//         implements InvoiceService {

//          private final OrderRepo orderRepo;

//         public InvoiceServiceImpl(OrderRepo orderRepo) {
//                 this.orderRepo = orderRepo;
//         }

//     @Override
//     public byte[] generateInvoicePdf(
//             Long orderId)
//             throws Exception {

//         Orders order =
//                 orderRepo.findById(orderId)
//                         .orElseThrow(() ->
//                                 new RuntimeException(
//                                         "Order not found"));

//         ByteArrayOutputStream baos =
//                 new ByteArrayOutputStream();

//         PdfWriter writer =
//                 new PdfWriter(baos);

//         PdfDocument pdfDocument =
//                 new PdfDocument(writer);

//         Document document =
//                 new Document(pdfDocument);

//         document.add(
//                 new Paragraph("INVOICE")
//                         .setBold()
//                         .setFontSize(20));

//         document.add(
//                 new Paragraph(
//                         "Order ID : "
//                                 + order.getOrderId()));

//         document.add(
//                 new Paragraph(
//                         "Total Amount : ₹"
//                                 + order.getOrderPrice()));

//         document.add(
//                 new Paragraph(" "));

//         Table table = new Table(2);

//         table.addCell("Course");

//         table.addCell("Price");

//         for (Course course :
//                 order.getCourses()) {

//             table.addCell(
//                     course.getCourseType());

//             table.addCell(
//                     "₹" +
//                     course.getCoursePrice());
//         }

//         document.add(table);

//         document.add(
//                 new Paragraph(" "));

//         document.add(
//                 new Paragraph(
//                         "Thank you for your purchase."));

//         document.close();

//         return baos.toByteArray();
//     }
// }

package com.examly.springapp.service;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Service;

import com.examly.springapp.model.Course;
import com.examly.springapp.model.Orders;
import com.examly.springapp.repository.OrderRepo;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final OrderRepo orderRepo;

    // Constructor Injection
    public InvoiceServiceImpl(OrderRepo orderRepo) {
        this.orderRepo = orderRepo;
    }

    @Override
    public byte[] generateInvoicePdf(Long orderId) throws Exception {

        // 1. Get order from database
        Orders order = orderRepo.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        // 2. Store generated PDF temporarily in memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);

        PdfDocument pdfDocument = new PdfDocument(writer);

        // A4 size
        pdfDocument.setDefaultPageSize(PageSize.A4);

        Document document = new Document(pdfDocument);

        // Page margins
        document.setMargins(40, 40, 40, 40);

        // Colors
        DeviceRgb primaryColor =
                new DeviceRgb(37, 99, 235);

        DeviceRgb lightBlue =
                new DeviceRgb(239, 246, 255);

        DeviceRgb lightGray =
                new DeviceRgb(248, 250, 252);

        /*
         * ============================
         * HEADER
         * ============================
         */

        Paragraph companyName = new Paragraph("KnowledgeNest")
                .setBold()
                .setFontSize(22)
                .setFontColor(primaryColor);

        document.add(companyName);

        Paragraph invoiceTitle = new Paragraph("INVOICE")
                .setBold()
                .setFontSize(28)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setTextAlignment(TextAlignment.RIGHT);

        document.add(invoiceTitle);

        document.add(
                new Paragraph("Course Purchase Invoice")
                        .setFontSize(11)
                        .setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.RIGHT)
        );

        document.add(new Paragraph("\n"));

        /*
         * ============================
         * ORDER DETAILS
         * ============================
         */

        Table orderDetails = new Table(
                UnitValue.createPercentArray(
                        new float[] {1, 2}))
                .useAllAvailableWidth();

        Cell orderLabel = new Cell()
                .add(new Paragraph("Order ID").setBold())
                .setBackgroundColor(lightBlue)
                .setPadding(10);

        Cell orderValue = new Cell()
                .add(new Paragraph(
                        String.valueOf(order.getOrderId())))
                .setPadding(10);

        orderDetails.addCell(orderLabel);
        orderDetails.addCell(orderValue);

        Cell totalLabel = new Cell()
                .add(new Paragraph("Total Amount").setBold())
                .setBackgroundColor(lightBlue)
                .setPadding(10);

        Cell totalValue = new Cell()
                .add(new Paragraph(
                        "₹" + order.getOrderPrice())
                        .setBold()
                        .setFontColor(primaryColor))
                .setPadding(10);

        orderDetails.addCell(totalLabel);
        orderDetails.addCell(totalValue);

        document.add(orderDetails);

        document.add(new Paragraph("\n"));

        /*
         * ============================
         * COURSE DETAILS TITLE
         * ============================
         */

        document.add(
                new Paragraph("Purchased Courses")
                        .setBold()
                        .setFontSize(16)
                        .setFontColor(ColorConstants.DARK_GRAY)
        );

        document.add(new Paragraph(" "));

        /*
         * ============================
         * COURSE TABLE
         * ============================
         */

        Table table = new Table(
                UnitValue.createPercentArray(
                        new float[] {3, 1}))
                .useAllAvailableWidth();

        // Header - Course
        Cell courseHeader = new Cell()
                .add(new Paragraph("Course").setBold())
                .setBackgroundColor(primaryColor)
                .setFontColor(ColorConstants.WHITE)
                .setPadding(10)
                .setTextAlignment(TextAlignment.LEFT);

        // Header - Price
        Cell priceHeader = new Cell()
                .add(new Paragraph("Price").setBold())
                .setBackgroundColor(primaryColor)
                .setFontColor(ColorConstants.WHITE)
                .setPadding(10)
                .setTextAlignment(TextAlignment.RIGHT);

        table.addHeaderCell(courseHeader);
        table.addHeaderCell(priceHeader);

        // Course rows
        for (Course course : order.getCourses()) {

            Cell courseCell = new Cell()
                    .add(new Paragraph(
                            course.getCourseType()))
                    .setBackgroundColor(lightGray)
                    .setPadding(10);

            Cell priceCell = new Cell()
                    .add(new Paragraph(
                            "₹" + course.getCoursePrice()))
                    .setBackgroundColor(lightGray)
                    .setPadding(10)
                    .setTextAlignment(TextAlignment.RIGHT);

            table.addCell(courseCell);
            table.addCell(priceCell);
        }

        document.add(table);

        document.add(new Paragraph("\n"));

        /*
         * ============================
         * GRAND TOTAL
         * ============================
         */

        Table totalTable = new Table(
                UnitValue.createPercentArray(
                        new float[] {3, 1}))
                .useAllAvailableWidth();

        Cell emptyCell = new Cell()
                .setBorder(null);

        Cell grandTotalCell = new Cell()
                .add(
                        new Paragraph(
                                "Total: ₹"
                                        + order.getOrderPrice())
                                .setBold()
                                .setFontSize(14)
                                .setFontColor(primaryColor)
                )
                .setBackgroundColor(lightBlue)
                .setBorder(
                        new SolidBorder(primaryColor, 1))
                .setPadding(12)
                .setTextAlignment(TextAlignment.RIGHT);

        totalTable.addCell(emptyCell);
        totalTable.addCell(grandTotalCell);

        document.add(totalTable);

        /*
         * ============================
         * FOOTER
         * ============================
         */

        document.add(new Paragraph("\n\n"));

        document.add(
                new Paragraph(
                        "Thank you for your purchase!")
                        .setBold()
                        .setFontSize(14)
                        .setTextAlignment(
                                TextAlignment.CENTER)
                        .setFontColor(primaryColor)
        );

        document.add(
                new Paragraph(
                        "Keep learning and keep growing.")
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(
                                TextAlignment.CENTER)
        );

        document.close();

        // 3. Convert generated PDF into byte[]
        return baos.toByteArray();
    }
}