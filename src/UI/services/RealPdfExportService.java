package UI.services;

import model.Bill;
import model.Payment;
import session.UserSession;

import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.io.File;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Real PDF export implementation using Java's built-in PrinterJob API.
 * Renders the receipt to a PDF file via the "Microsoft Print to PDF" printer
 * (available on Windows 10+), or any PDF printer on the system.
 * Falls back to direct printing if no PDF printer is found.
 *
 * No external library required — uses only java.awt.print.
 */
public class RealPdfExportService implements PdfExportService {

    // ── Data holders passed in before export ─────────────────────────────────
    private Bill    bill;
    private Payment payment;
    private String  collectorName;

    /** Call this before exportPaymentReceipt() to supply receipt data. */
    public void setReceiptData(Bill bill, Payment payment, String collectorName) {
        this.bill          = bill;
        this.payment       = payment;
        this.collectorName = collectorName;
    }

    // =========================================================================
    // PdfExportService — exportPaymentReceipt
    // =========================================================================
    @Override
    public boolean exportPaymentReceipt(String paymentId, String outputPath) {
        if (bill == null || payment == null) return false;

        // Use Java2D printing to render the receipt
        PrinterJob job = PrinterJob.getPrinterJob();

        // Page format: A5 portrait (148mm x 210mm) in points (1pt = 1/72 inch)
        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();
        double a5w = cmToPt(14.8);
        double a5h = cmToPt(21.0);
        paper.setSize(a5w, a5h);
        double margin = cmToPt(1.5);
        paper.setImageableArea(margin, margin, a5w - 2 * margin, a5h - 2 * margin);
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);

        // Build the Printable
        ReceiptPrintable printable = new ReceiptPrintable(bill, payment, collectorName);
        job.setPrintable(printable, pf);
        job.setJobName("BienLai_" + paymentId);

        // Try to find a PDF printer (Windows: "Microsoft Print to PDF")
        javax.print.PrintService pdfPrinter = findPdfPrinter();
        if (pdfPrinter != null) {
            try {
                job.setPrintService(pdfPrinter);
                // Set output file via DocFlavor attribute
                javax.print.attribute.PrintRequestAttributeSet attrs =
                    new javax.print.attribute.HashPrintRequestAttributeSet();

                File outFile = new File(outputPath);
                outFile.getParentFile().mkdirs();
                attrs.add(new javax.print.attribute.standard.Destination(outFile.toURI()));
                attrs.add(javax.print.attribute.standard.MediaSizeName.ISO_A5);
                attrs.add(javax.print.attribute.standard.OrientationRequested.PORTRAIT);
                job.print(attrs);
                return true;
            } catch (PrinterException ex) {
                System.err.println("[PDF] PDF printer failed: " + ex.getMessage());
                // Fall through to dialog-based export
            }
        }

        // No silent PDF printer → show print dialog so user can choose "Save as PDF"
        if (job.printDialog()) {
            try {
                job.print();
                return true;
            } catch (PrinterException ex) {
                System.err.println("[PDF] Print failed: " + ex.getMessage());
                return false;
            }
        }
        return false;
    }

    // =========================================================================
    // PdfExportService — stubs for invoice / report (not used in PaymentPanel)
    // =========================================================================
    @Override
    public boolean exportInvoice(String invoiceId, String outputPath) {
        return exportPaymentReceipt(invoiceId, outputPath);
    }

    @Override
    public boolean exportMonthlyReport(int month, int year, String outputPath) {
        return false; // Not implemented in this service
    }

    // =========================================================================
    // HELPER — find "Microsoft Print to PDF" or similar
    // =========================================================================
    private javax.print.PrintService findPdfPrinter() {
        javax.print.PrintService[] services =
            javax.print.PrintServiceLookup.lookupPrintServices(null, null);
        for (javax.print.PrintService svc : services) {
            String name = svc.getName().toLowerCase();
            if (name.contains("pdf") || name.contains("print to pdf")) {
                return svc;
            }
        }
        return null;
    }

    private static double cmToPt(double cm) { return cm / 2.54 * 72.0; }

    public static RealPdfExportService getInstance() { return new RealPdfExportService(); }

    // =========================================================================
    // INNER CLASS — Printable receipt renderer
    // =========================================================================
    private static class ReceiptPrintable implements Printable {

        private final Bill    bill;
        private final Payment payment;
        private final String  collector;

        ReceiptPrintable(Bill bill, Payment payment, String collector) {
            this.bill      = bill;
            this.payment   = payment;
            this.collector = collector;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
                throws PrinterException {
            if (pageIndex > 0) return NO_SUCH_PAGE;

            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                               RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);

            // Origin at imageable area
            double ox = pageFormat.getImageableX();
            double oy = pageFormat.getImageableY();
            double iw = pageFormat.getImageableWidth();
            g.translate(ox, oy);

            float x  = 0;
            float y  = 0;
            float w  = (float) iw;

            // ── Fonts ────────────────────────────────────────────────────────
            Font fontTitle     = new Font("SansSerif", Font.BOLD,  16);
            Font fontSubtitle  = new Font("SansSerif", Font.PLAIN, 10);
            Font fontLabel     = new Font("SansSerif", Font.BOLD,  11);
            Font fontValue     = new Font("SansSerif", Font.PLAIN, 11);
            Font fontTotal     = new Font("SansSerif", Font.BOLD,  13);
            Font fontTotalAmt  = new Font("SansSerif", Font.BOLD,  15);
            Font fontSig       = new Font("SansSerif", Font.PLAIN,  9);
            Font fontThanks    = new Font("SansSerif", Font.ITALIC, 9);

            // ── Title ────────────────────────────────────────────────────────
            g.setFont(fontTitle);
            g.setColor(Color.BLACK);
            centerText(g, "BIÊN LAI THANH TOÁN", x, y, w);
            y += 20;

            g.setFont(fontSubtitle);
            g.setColor(new Color(0x555555));
            centerText(g, "CÔNG TY ĐIỆN LỰC G-LIGHT", x, y, w);
            y += 8;

            // ── Divider ──────────────────────────────────────────────────────
            y += 6;
            drawDashedLine(g, x, y, x + w, y);
            y += 10;

            // ── Receipt rows ─────────────────────────────────────────────────
            String dt = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
            NumberFormat nf = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

            String[][] rows = {
                {"Mã hóa đơn:", bill.getBillCode()},
                {"Hộ gia đình:", bill.getOwnerName() + " (" + bill.getHouseholdCode() + ")"},
                {"Kỳ hóa đơn:", bill.getPeriodDisplay()},
                {"Phương thức:", payment.getPaymentMethod()},
                {"Ngày thu:", dt},
                {"Người thu:", collector},
            };

            float rowH = 18;
            for (String[] row : rows) {
                g.setFont(fontLabel);
                g.setColor(new Color(0x444444));
                g.drawString(row[0], x, y + 12);

                g.setFont(fontValue);
                g.setColor(Color.BLACK);
                // Right-align value
                FontMetrics fm = g.getFontMetrics();
                int valW = fm.stringWidth(row[1]);
                g.drawString(row[1], x + w - valW, y + 12);
                y += rowH;
            }

            // ── Total ────────────────────────────────────────────────────────
            y += 4;
            drawDashedLine(g, x, y, x + w, y);
            y += 14;

            String totalStr = nf.format((long) bill.getTotalAmount()) + " đ";
            g.setFont(fontTotal);
            g.setColor(Color.BLACK);
            g.drawString("TỔNG TIỀN:", x, y);
            g.setFont(fontTotalAmt);
            g.setColor(new Color(0x2E7D32));
            FontMetrics fmTotal = g.getFontMetrics();
            g.drawString(totalStr, x + w - fmTotal.stringWidth(totalStr), y);
            y += 8;

            drawDashedLine(g, x, y, x + w, y);
            y += 28;

            // ── Signature area ───────────────────────────────────────────────
            float halfW = w / 2;
            g.setFont(fontSig);
            g.setColor(Color.BLACK);
            centerText(g, "Khách hàng", x,        y, halfW);
            centerText(g, "Nhân viên thu tiền", x + halfW, y, halfW);
            y += 14;
            g.setColor(new Color(0x888888));
            centerText(g, "(Ký, ghi rõ họ tên)", x, y, halfW);
            centerText(g, collector,               x + halfW, y, halfW);

            y += 55;

            // ── Thank-you footer ─────────────────────────────────────────────
            g.setFont(fontThanks);
            g.setColor(new Color(0x888888));
            centerText(g, "Cảm ơn quý khách đã thanh toán tiền điện!", x, y, w);

            return PAGE_EXISTS;
        }

        // Render string centered in [x, x+w]
        private void centerText(Graphics2D g, String text, float x, float y, float w) {
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(text);
            g.drawString(text, x + (w - tw) / 2f, y + fm.getAscent());
        }

        // Draw a dashed horizontal line
        private void drawDashedLine(Graphics2D g, float x1, float y, float x2, float y2) {
            Stroke old = g.getStroke();
            g.setStroke(new BasicStroke(0.7f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                    1f, new float[]{4f, 4f}, 0f));
            g.setColor(new Color(0xAAAAAA));
            g.drawLine((int) x1, (int) y, (int) x2, (int) y2);
            g.setStroke(old);
        }
    }
}
