package util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Pure-Java OpenXML Spreadsheet (.xlsx) Exporter.
 * No external dependencies (like Apache POI) required.
 * Writes formatted zip archives directly containing required XML structures.
 */
public class XlsxExporter {

    /**
     * Exports tabular data dynamically into a professional .xlsx file.
     *
     * @param file      Destination file on disk
     * @param sheetName Name of the worksheet (e.g. "Báo cáo")
     * @param headers   Column header labels
     * @param rows      List of row data (Object[] for cells)
     * @throws IOException if writing fails
     */
    public static void export(File file, String sheetName, String[] headers, List<Object[]> rows) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos)) {

            // 1. Write _rels/.rels
            writeRels(zos);

            // 2. Write [Content_Types].xml
            writeContentTypes(zos);

            // 3. Write xl/workbook.xml
            writeWorkbook(zos, sheetName);

            // 4. Write xl/_rels/workbook.xml.rels
            writeWorkbookRels(zos);

            // 5. Write xl/worksheets/sheet1.xml (the actual data sheet)
            writeWorksheet(zos, headers, rows);
            
            zos.finish();
        }
    }

    private static void writeRels(ZipOutputStream zos) throws IOException {
        zos.putNextEntry(new ZipEntry("_rels/.rels"));
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                     "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                     "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>\n" +
                     "</Relationships>";
        zos.write(xml.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static void writeContentTypes(ZipOutputStream zos) throws IOException {
        zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                     "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                     "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
                     "  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
                     "  <Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>\n" +
                     "  <Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n" +
                     "</Types>";
        zos.write(xml.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static void writeWorkbook(ZipOutputStream zos, String sheetName) throws IOException {
        zos.putNextEntry(new ZipEntry("xl/workbook.xml"));
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                     "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n" +
                     "  <sheets>\n" +
                     "    <sheet name=\"" + escapeXml(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/>\n" +
                     "  </sheets>\n" +
                     "</workbook>";
        zos.write(xml.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static void writeWorkbookRels(ZipOutputStream zos) throws IOException {
        zos.putNextEntry(new ZipEntry("xl/_rels/workbook.xml.rels"));
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                     "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                     "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>\n" +
                     "</Relationships>";
        zos.write(xml.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static void writeWorksheet(ZipOutputStream zos, String[] headers, List<Object[]> rows) throws IOException {
        zos.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
        
        Writer writer = new OutputStreamWriter(zos, StandardCharsets.UTF_8);
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        writer.write("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n");

        // 1. Calculate dynamic column widths (Auto-size)
        int numCols = headers.length;
        int[] colWidths = new int[numCols];
        for (int i = 0; i < numCols; i++) {
            colWidths[i] = Math.max(12, headers[i].length() + 3);
        }
        for (Object[] row : rows) {
            for (int i = 0; i < numCols && i < row.length; i++) {
                if (row[i] != null) {
                    colWidths[i] = Math.max(colWidths[i], row[i].toString().length() + 3);
                }
            }
        }

        // Apply width cap
        writer.write("  <cols>\n");
        for (int i = 0; i < numCols; i++) {
            int w = Math.min(55, colWidths[i]);
            writer.write("    <col min=\"" + (i + 1) + "\" max=\"" + (i + 1) + "\" width=\"" + w + "\" customWidth=\"1\"/>\n");
        }
        writer.write("  </cols>\n");

        // 2. Sheet Data
        writer.write("  <sheetData>\n");

        // Row 1: Headers
        writer.write("    <row r=\"1\" customHeight=\"1\" ht=\"24\">\n");
        for (int i = 0; i < numCols; i++) {
            String cellRef = getColLetter(i) + "1";
            writer.write("      <c r=\"" + cellRef + "\" t=\"inlineStr\">\n");
            writer.write("        <is><t>" + escapeXml(headers[i]) + "</t></is>\n");
            writer.write("      </c>\n");
        }
        writer.write("    </row>\n");

        // Rows 2+: Values
        int rowIdx = 2;
        for (Object[] row : rows) {
            writer.write("    <row r=\"" + rowIdx + "\">\n");
            for (int i = 0; i < numCols; i++) {
                String cellRef = getColLetter(i) + rowIdx;
                Object val = (i < row.length) ? row[i] : null;

                if (val == null) {
                    // Empty cell
                    writer.write("      <c r=\"" + cellRef + "\"/>\n");
                } else if (val instanceof Number) {
                    // Numeric cell type
                    writer.write("      <c r=\"" + cellRef + "\" t=\"n\">\n");
                    writer.write("        <v>" + val + "</v>\n");
                    writer.write("      </c>\n");
                } else {
                    // String cell type using inline strings
                    writer.write("      <c r=\"" + cellRef + "\" t=\"inlineStr\">\n");
                    writer.write("        <is><t>" + escapeXml(val.toString()) + "</t></is>\n");
                    writer.write("      </c>\n");
                }
            }
            writer.write("    </row>\n");
            rowIdx++;
        }

        writer.write("  </sheetData>\n");
        writer.write("</worksheet>");
        writer.flush();
        zos.closeEntry();
    }

    private static String getColLetter(int colIndex) {
        StringBuilder sb = new StringBuilder();
        while (colIndex >= 0) {
            sb.insert(0, (char) ('A' + (colIndex % 26)));
            colIndex = (colIndex / 26) - 1;
        }
        return sb.toString();
    }

    private static String escapeXml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
}
