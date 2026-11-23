package com.orderflow.order.invoice.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.orderflow.order.invoice.entities.Invoice;
import com.orderflow.order.invoice.entities.InvoiceLine;
import com.orderflow.order.invoice.entities.InvoiceTax;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class InvoicePdfGenerator {

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.of("Asia/Kolkata"));

    public byte[] generate(Invoice inv) {
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph("TAX INVOICE", TITLE));
            doc.add(new Paragraph(" "));

            PdfPTable head = new PdfPTable(2);
            head.setWidthPercentage(100);
            head.addCell(sellerCell(inv.getSellerName(),
                    inv.getSellerAddress() + "\nGSTIN: " + inv.getSellerGstin()));
            head.addCell(right(plainText("Invoice No: " + inv.getInvoiceNumber()
                    + "\nDate: " + DATE.format(inv.getIssuedAt())
                    + "\nOrder ID: " + inv.getOrderId()
                    + "\nBill to: " + inv.getCustomerEmail())));
            doc.add(head);
            doc.add(new Paragraph(" "));

            PdfPTable items = new PdfPTable(new float[]{5, 1.2f, 2, 2});
            items.setWidthPercentage(100);
            header(items, "Item", Element.ALIGN_LEFT);
            header(items, "Qty", Element.ALIGN_RIGHT);
            header(items, "Unit price", Element.ALIGN_RIGHT);
            header(items, "Amount", Element.ALIGN_RIGHT);
            for (InvoiceLine l : inv.getLines()) {
                body(items, l.getDescription(), Element.ALIGN_LEFT);
                body(items, String.valueOf(l.getQuantity()), Element.ALIGN_RIGHT);
                body(items, money(l.getUnitPrice()), Element.ALIGN_RIGHT);
                body(items, money(l.getLineTotal()), Element.ALIGN_RIGHT);
            }
            doc.add(items);
            doc.add(new Paragraph(" "));

            PdfPTable sum = new PdfPTable(new float[]{7, 3});
            sum.setWidthPercentage(60);
            sum.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryRow(sum, "Taxable value", money(inv.getTaxableAmount()), false);
            for (InvoiceTax t : inv.getTaxes()) {
                summaryRow(sum, t.getName() + " @ " + t.getRate().stripTrailingZeros().toPlainString() + "%",
                        money(t.getAmount()), false);
            }
            summaryRow(sum, "TOTAL (" + inv.getCurrency() + ", incl. tax)", money(inv.getTotalAmount()), true);
            doc.add(sum);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Prices are inclusive of tax. This is a computer generated invoice.", SMALL));

            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Error in PDF preparation", e);
        }
        return out.toByteArray();
    }

    private static String money(BigDecimal v) {
        return String.format(Locale.ENGLISH, "%,.2f", v);
    }

    private static Phrase plainText(String s) {
        return new Phrase(s, NORMAL);
    }

    private static PdfPCell sellerCell(String name, String details) {
        Phrase p = new Phrase();
        p.add(new Chunk(name + "\n", BOLD));
        p.add(new Chunk(details, NORMAL));
        PdfPCell c = new PdfPCell(p);
        c.setBorder(PdfPCell.NO_BORDER);
        return c;
    }

    private static PdfPCell right(Phrase p) {
        PdfPCell c = new PdfPCell(p);
        c.setBorder(PdfPCell.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private static void header(PdfPTable t, String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, BOLD));
        c.setBackgroundColor(new Color(230, 230, 230));
        c.setHorizontalAlignment(align);
        c.setPadding(6);
        t.addCell(c);
    }

    private static void body(PdfPTable t, String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, NORMAL));
        c.setHorizontalAlignment(align);
        c.setPadding(6);
        t.addCell(c);
    }

    private static void summaryRow(PdfPTable t, String label, String value, boolean strong) {
        Font f = strong ? BOLD : NORMAL;
        PdfPCell a = new PdfPCell(new Phrase(label, f));
        PdfPCell b = new PdfPCell(new Phrase(value, f));
        b.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (strong) {
            a.setBackgroundColor(new Color(240, 240, 240));
            b.setBackgroundColor(new Color(240, 240, 240));
        }
        a.setPadding(6);
        b.setPadding(6);
        t.addCell(a);
        t.addCell(b);
    }
}
