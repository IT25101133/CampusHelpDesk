package com.sliit.helpdesk.report.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.report.dto.ReportSummaryResponse;
import com.sliit.helpdesk.report.dto.TicketReportCriteria;
import com.sliit.helpdesk.ticket.model.Ticket;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF stays in memory because it is a short summary.
 * Excel is written with SXSSF so rows are flushed instead of held in one workbook.
 */
@Service
public class ReportExportService {

    public static final MediaType EXCEL_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    public static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final ReportService reportService;

    public ReportExportService(ReportService reportService) {
        this.reportService = reportService;
    }

    public byte[] exportPdf() {
        return toPdf(reportService.summary());
    }

    public String pdfFileName() {
        return "report-summary.pdf";
    }

    public String excelFileName() {
        return safeFileName("ticket-report-" + LocalDateTime.now().format(FILE_STAMP)) + ".xlsx";
    }

    /**
     * Streams the workbook to the response. The caller must not catch and hide the IOException.
     */
    public void writeExcel(
            HttpServletResponse response,
            User generatedBy,
            TicketReportCriteria criteria,
            List<Ticket> tickets
    ) throws IOException {
        String filename = excelFileName();
        response.setContentType(EXCEL_CONTENT_TYPE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);
        try {
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper helper = workbook.getCreationHelper();
            dateStyle.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));

            writeCover(workbook, headerStyle, dateStyle, generatedBy, criteria, tickets.size());
            writeSummary(workbook, headerStyle, tickets);
            writeTickets(workbook, headerStyle, dateStyle, tickets);
            writeCategories(workbook, headerStyle, reportService.byCategory());
            writeStaff(workbook, headerStyle, reportService.staffPerformance(tickets));
            writeOverdue(workbook, headerStyle, dateStyle, reportService.overdue(tickets, LocalDateTime.now()));
            workbook.write(response.getOutputStream());
            response.flushBuffer();
        } finally {
            workbook.dispose();
            workbook.close();
        }
    }

    private void writeCover(
            SXSSFWorkbook workbook,
            CellStyle headerStyle,
            CellStyle dateStyle,
            User generatedBy,
            TicketReportCriteria criteria,
            int rowCount
    ) {
        Sheet sheet = workbook.createSheet("Cover");
        // Excel row 1 lists the filters and the time the file was built.
        Row summary = sheet.createRow(0);
        summary.createCell(0).setCellValue("Generated at");
        Cell generatedAt = summary.createCell(1);
        LocalDateTime generated = LocalDateTime.now();
        generatedAt.setCellValue(java.sql.Timestamp.valueOf(generated));
        generatedAt.setCellStyle(dateStyle);
        summary.createCell(2).setCellValue("Filters");
        summary.createCell(3).setCellValue(filterSummary(criteria));
        String[] headers = {"Field", "Value"};
        Row header = sheet.createRow(2);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        int row = 3;
        row = coverRow(sheet, row, "Generated at", LocalDateTime.now(), dateStyle);
        row = coverRow(sheet, row, "Generated by", generatedBy == null ? "Unknown" : generatedBy.getFullName(), null);
        row = coverRow(sheet, row, "Category", criteria.getCategoryLabel(), null);
        row = coverRow(sheet, row, "Department", criteria.getDepartment() == null ? "All departments" : criteria.getDepartment(), null);
        row = coverRow(sheet, row, "Status", criteria.getStatuses().isEmpty() ? "Any status" : criteria.getStatuses().toString(), null);
        row = coverRow(sheet, row, "Priority", criteria.getPriorities().isEmpty() ? "Any priority" : criteria.getPriorities().toString(), null);
        row = coverRow(sheet, row, "From", criteria.getFrom(), dateStyle);
        row = coverRow(sheet, row, "To", criteria.getTo(), dateStyle);
        row = coverRow(sheet, row, "Assigned to", criteria.getAssignedToLabel(), null);
        row = coverRow(sheet, row, "Created by", criteria.getCreatedByLabel(), null);
        coverRow(sheet, row, "Matching tickets", rowCount, null);
        sheet.setColumnWidth(0, 22 * 256);
        sheet.setColumnWidth(1, 42 * 256);
        sheet.setColumnWidth(2, 16 * 256);
        sheet.setColumnWidth(3, 72 * 256);
    }

    private static String filterSummary(TicketReportCriteria criteria) {
        String category = criteria.getCategoryLabel() == null ? "All categories" : criteria.getCategoryLabel();
        String department = criteria.getDepartment() == null ? "All departments" : criteria.getDepartment();
        String status = criteria.getStatuses().isEmpty() ? "Any status" : criteria.getStatuses().toString();
        String priority = criteria.getPriorities().isEmpty() ? "Any priority" : criteria.getPriorities().toString();
        String from = criteria.getFrom() == null ? "Any start" : criteria.getFrom().toLocalDate().toString();
        String to = criteria.getTo() == null ? "Any end" : criteria.getTo().toLocalDate().toString();
        return "category=" + category
                + "; department=" + department
                + "; status=" + status
                + "; priority=" + priority
                + "; from=" + from
                + "; to=" + to;
    }

    private void writeSummary(SXSSFWorkbook workbook, CellStyle headerStyle, List<Ticket> tickets) {
        Sheet sheet = workbook.createSheet("Summary");
        writeHeader(sheet, headerStyle, "Metric", "Value");
        long open = tickets.stream().filter(ticket -> ticket.getStatus() != null && ticket.getStatus().name().equals("OPEN")).count();
        long pending = tickets.stream().filter(ticket -> ticket.getStatus() != null && ticket.getStatus().name().equals("IN_PROGRESS")).count();
        long resolved = tickets.stream().filter(ticket -> ticket.getStatus() != null
                && (ticket.getStatus().name().equals("RESOLVED") || ticket.getStatus().name().equals("CLOSED"))).count();
        writeTextRow(sheet, 1, "Total tickets", tickets.size());
        writeTextRow(sheet, 2, "Open tickets", open);
        writeTextRow(sheet, 3, "Pending tickets", pending);
        writeTextRow(sheet, 4, "Resolved tickets", resolved);
        sheet.setColumnWidth(0, 24 * 256);
        sheet.setColumnWidth(1, 18 * 256);
    }

    private void writeTickets(SXSSFWorkbook workbook, CellStyle headerStyle, CellStyle dateStyle, List<Ticket> tickets) {
        Sheet sheet = workbook.createSheet("Tickets");
        writeHeader(sheet, headerStyle, "Ticket", "Title", "Category", "Department", "Status", "Priority", "Assigned to", "Created by", "Created at");
        int index = 1;
        for (Ticket ticket : tickets) {
            Row row = sheet.createRow(index++);
            row.createCell(0).setCellValue(ticket.getTicketNumber());
            row.createCell(1).setCellValue(ticket.getTitle() == null ? "" : ticket.getTitle());
            row.createCell(2).setCellValue(ticket.getCategory() == null ? "" : ticket.getCategory().getName());
            row.createCell(3).setCellValue(ticket.getCategory() == null || ticket.getCategory().getDepartment() == null
                    ? "" : ticket.getCategory().getDepartment());
            row.createCell(4).setCellValue(ticket.getStatus() == null ? "" : ticket.getStatus().name());
            row.createCell(5).setCellValue(ticket.getPriority() == null ? "" : ticket.getPriority().name());
            row.createCell(6).setCellValue(ticket.getAssignee() == null ? "" : ticket.getAssignee().getFullName());
            row.createCell(7).setCellValue(ticket.getRequester() == null ? "" : ticket.getRequester().getFullName());
            Cell created = row.createCell(8);
            if (ticket.getCreatedAt() != null) {
                created.setCellValue(java.sql.Timestamp.valueOf(ticket.getCreatedAt()));
                created.setCellStyle(dateStyle);
            }
        }
        for (int column = 0; column < 9; column++) {
            sheet.setColumnWidth(column, 18 * 256);
        }
    }

    private void writeCategories(SXSSFWorkbook workbook, CellStyle headerStyle, List<com.sliit.helpdesk.report.dto.CategoryCountResponse> rows) {
        Sheet sheet = workbook.createSheet("Categories");
        writeHeader(sheet, headerStyle, "Category", "Tickets");
        int index = 1;
        for (var row : rows) {
            writeTextRow(sheet, index++, row.getCategory(), row.getTicketCount());
        }
        sheet.setColumnWidth(0, 28 * 256);
        sheet.setColumnWidth(1, 14 * 256);
    }

    private void writeStaff(SXSSFWorkbook workbook, CellStyle headerStyle, List<com.sliit.helpdesk.report.dto.StaffPerformanceResponse> rows) {
        Sheet sheet = workbook.createSheet("Staff");
        writeHeader(sheet, headerStyle, "Staff", "Resolved");
        int index = 1;
        for (var row : rows) {
            writeTextRow(sheet, index++, row.getStaffName(), row.getTicketsResolved());
        }
        sheet.setColumnWidth(0, 28 * 256);
        sheet.setColumnWidth(1, 14 * 256);
    }

    private void writeOverdue(SXSSFWorkbook workbook, CellStyle headerStyle, CellStyle dateStyle, List<com.sliit.helpdesk.report.dto.OverdueTicketResponse> rows) {
        Sheet sheet = workbook.createSheet("Overdue");
        writeHeader(sheet, headerStyle, "Ticket", "Category", "Status", "Hours overdue", "Created at");
        int index = 1;
        for (var item : rows) {
            Row row = sheet.createRow(index++);
            row.createCell(0).setCellValue(item.getTicketNumber() == null ? "" : item.getTicketNumber());
            row.createCell(1).setCellValue(item.getCategory() == null ? "" : item.getCategory());
            row.createCell(2).setCellValue(item.getStatus() == null ? "" : item.getStatus().name());
            row.createCell(3).setCellValue(item.getHoursOverdue());
            Cell created = row.createCell(4);
            if (item.getCreatedAt() != null) {
                created.setCellValue(java.sql.Timestamp.valueOf(item.getCreatedAt()));
                created.setCellStyle(dateStyle);
            }
        }
        for (int column = 0; column < 5; column++) {
            sheet.setColumnWidth(column, 20 * 256);
        }
    }

    private static void writeHeader(Sheet sheet, CellStyle headerStyle, String... titles) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < titles.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(titles[i]);
            cell.setCellStyle(headerStyle);
        }
        sheet.createFreezePane(0, 1);
    }

    private static int coverRow(Sheet sheet, int index, String field, Object value, CellStyle dateStyle) {
        Row row = sheet.createRow(index);
        row.createCell(0).setCellValue(field);
        Cell cell = row.createCell(1);
        if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(java.sql.Timestamp.valueOf(dateTime));
            if (dateStyle != null) {
                cell.setCellStyle(dateStyle);
            }
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : String.valueOf(value));
        }
        return index + 1;
    }

    private static void writeTextRow(Sheet sheet, int index, String label, Object value) {
        Row row = sheet.createRow(index);
        row.createCell(0).setCellValue(label);
        if (value instanceof Number number) {
            row.createCell(1).setCellValue(number.doubleValue());
        } else {
            row.createCell(1).setCellValue(value == null ? "" : String.valueOf(value));
        }
    }

    /**
     * Keeps filenames ASCII so older Excel builds do not reject the download.
     */
    static String safeFileName(String name) {
        String ascii = name == null ? "" : name.replaceAll("[^A-Za-z0-9._-]", "_");
        return ascii.isBlank() ? "report" : ascii;
    }

    private byte[] toPdf(ReportSummaryResponse summary) {
        Document document = new Document();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, out);
            document.open();
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Paragraph title = new Paragraph("Campus Help Desk — Summary Report", titleFont);
            title.setSpacingAfter(12);
            document.add(title);
            document.add(new Paragraph("Generated at " + LocalDateTime.now().format(GENERATED_AT)));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.addCell(headerCell("Metric", headerFont));
            table.addCell(headerCell("Value", headerFont));
            addPdfRow(table, "Total tickets", summary.getTotalTickets());
            addPdfRow(table, "Open tickets", summary.getOpenTickets());
            addPdfRow(table, "Pending tickets", summary.getPendingTickets());
            addPdfRow(table, "Resolved tickets", summary.getResolvedTickets());
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            document.close();
            throw new IllegalStateException("Could not export PDF report", ex);
        }
    }

    private static PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private static void addPdfRow(PdfPTable table, String metric, long value) {
        table.addCell(metric);
        table.addCell(String.valueOf(value));
    }
}
