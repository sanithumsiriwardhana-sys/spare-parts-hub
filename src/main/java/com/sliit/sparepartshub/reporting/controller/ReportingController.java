package com.sliit.sparepartshub.reporting.controller;

import com.sliit.sparepartshub.entity.AuditLog;
import com.sliit.sparepartshub.reporting.dto.AnomalyEvent;
import com.sliit.sparepartshub.reporting.dto.SalesReport;
import com.sliit.sparepartshub.reporting.service.AuditService;
import com.sliit.sparepartshub.reporting.service.SalesReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Function 6 (Admin side) - Centralized Reporting & Audit Logging.
 * Route prefix /reporting is already restricted to ROLE_ADMIN in
 * SecurityConfig - don't duplicate that check here.
 */
@Controller
public class ReportingController {

    private final SalesReportService salesReportService;
    private final AuditService auditService;

    public ReportingController(SalesReportService salesReportService, AuditService auditService) {
        this.salesReportService = salesReportService;
        this.auditService = auditService;
    }

    @GetMapping("/reporting")
    public String index() {
        return "reporting/index";
    }

    // A2/A3: default to "today" if no dates are given, matching the
    // daily-report case; weekly/monthly is just a wider from/to range
    // picked in the form, no separate endpoint needed.
    @GetMapping("/reporting/sales")
    public String salesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        LocalDate effectiveFrom = (from != null) ? from : LocalDate.now();
        LocalDate effectiveTo = (to != null) ? to : LocalDate.now();

        LocalDateTime start = effectiveFrom.atStartOfDay();
        LocalDateTime end = effectiveTo.plusDays(1).atStartOfDay(); // inclusive of the "to" day

        SalesReport report = salesReportService.generateReport(start, end);

        model.addAttribute("report", report);
        model.addAttribute("from", effectiveFrom);
        model.addAttribute("to", effectiveTo);
        return "reporting/sales-report";
    }

    // A4a: CSV export of whatever range is currently being viewed.
    @GetMapping("/reporting/sales/export")
    public ResponseEntity<String> exportSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        SalesReport report = salesReportService.generateReport(start, end);
        String csv = salesReportService.toCsv(report);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"sales-report-" + from + "-to-" + to + ".csv\"")
                .body(csv);
    }

    // A5/A6: anomaly dashboard - currently ships with the price-drop rule.
    @GetMapping("/reporting/audit")
    public String anomalyDashboard(Model model) {
        List<AnomalyEvent> anomalies = auditService.detectPriceDropAnomalies();
        model.addAttribute("anomalies", anomalies);
        return "reporting/audit-dashboard";
    }

    // A7: full audit log, paged, for when Admin wants to browse beyond
    // just the flagged anomalies.
    @GetMapping("/reporting/audit/log")
    public String auditLog(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<AuditLog> logs = auditService.getAuditLog(PageRequest.of(page, 25));
        model.addAttribute("logs", logs);
        return "reporting/audit-log";
    }

    // A7: detail view for one event - full before/after values.
    @GetMapping("/reporting/audit/{logId}")
    public String eventDetail(org.springframework.web.bind.annotation.PathVariable Integer logId, Model model) {
        model.addAttribute("event", auditService.getEvent(logId));
        return "reporting/audit-event-detail";
    }
}

