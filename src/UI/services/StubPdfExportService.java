package UI.services;

import UI.theme.ThemeManager;

/**
 * Stub PDF implementation. Replace with RealPdfExportService when adding iText/PDFBox.
 */
public class StubPdfExportService implements PdfExportService {

    @Override
    public boolean exportInvoice(String invoiceId, String outputPath) {
        ThemeManager.showInfoDialog(null,
            "<html><b>Xuất PDF Hóa đơn " + invoiceId + "</b><br/>" +
            "Đường dẫn: " + outputPath + "<br/><i>(Thêm thư viện PDF để kích hoạt.)</i></html>",
            "Xuất PDF");
        return true;
    }

    @Override
    public boolean exportMonthlyReport(int month, int year, String outputPath) {
        ThemeManager.showInfoDialog(null,
            "<html><b>Xuất báo cáo tháng " + month + "/" + year + "</b><br/>" +
            "Đường dẫn: " + outputPath + "<br/><i>(Thêm thư viện PDF để kích hoạt.)</i></html>",
            "Xuất Báo cáo");
        return true;
    }

    @Override
    public boolean exportPaymentReceipt(String paymentId, String outputPath) {
        ThemeManager.showInfoDialog(null,
            "<html><b>Xuất biên lai " + paymentId + "</b><br/>" +
            "Đường dẫn: " + outputPath + "<br/><i>(Thêm thư viện PDF để kích hoạt.)</i></html>",
            "Xuất Biên lai");
        return true;
    }

    public static PdfExportService getInstance() { return new StubPdfExportService(); }
}
