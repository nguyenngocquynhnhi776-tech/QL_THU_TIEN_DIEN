package UI.services;

/**
 * PDF Export Service interface.
 * Designed for dependency injection — swap StubPdfExportService for a real
 * implementation (e.g. backed by Apache PDFBox or iText) without touching any UI code.
 */
public interface PdfExportService {

    /**
     * Export an invoice to PDF.
     * @param invoiceId   unique invoice identifier
     * @param outputPath  file system path to write the PDF to (e.g. "C:/output/INV-001.pdf")
     * @return true if export succeeded
     */
    boolean exportInvoice(String invoiceId, String outputPath);

    /**
     * Export a monthly revenue report to PDF.
     * @param month      month number (1–12)
     * @param year       4-digit year
     * @param outputPath destination file path
     * @return true if export succeeded
     */
    boolean exportMonthlyReport(int month, int year, String outputPath);

    /**
     * Export a payment receipt to PDF.
     * @param paymentId  unique payment identifier
     * @param outputPath destination file path
     * @return true if export succeeded
     */
    boolean exportPaymentReceipt(String paymentId, String outputPath);
}
