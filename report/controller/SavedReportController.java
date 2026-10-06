package com.sliit.helpdesk.report.controller;

// Saved Report Controller is part of the campus help desk controller code.

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.report.dto.SavedReportForm;
import com.sliit.helpdesk.report.model.Report;
import com.sliit.helpdesk.report.service.AuditService;
import com.sliit.helpdesk.report.service.SavedReportService;
import com.sliit.helpdesk.ticket.model.TicketPriority;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'DEPT_HEAD')")
public class SavedReportController {

    private final SavedReportService savedReportService;
    private final AuthService authService;
    private final AuditService auditService;

    public SavedReportController(
            SavedReportService savedReportService,
            AuthService authService,
            AuditService auditService
    ) {
        this.savedReportService = savedReportService;
        this.authService = authService;
        this.auditService = auditService;
    }

    @GetMapping({"/reports/saved", "/admin/reports"})
    public String saved(Model model) {
        model.addAttribute("reports", savedReportService.history());
        model.addAttribute("departments", savedReportService.departments());
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("priorities", TicketPriority.values());
        return "report/saved-reports";
    }

    @PostMapping({"/reports/saved/generate", "/admin/reports/generate"})
    public String generate(
            Authentication authentication,
            @RequestParam String name,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            RedirectAttributes redirectAttributes
    ) {
        User user = authService.requireByEmail(authentication.getName());
        try {
            Report report = savedReportService.generate(user, name, department, status, priority);
            auditService.log(user, "Generated report " + report.getName(), "REPORT", report.getId());
            redirectAttributes.addFlashAttribute("success",
                    "Report generated — " + report.getTicketCount() + " ticket(s) matched.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reports/saved";
    }

    @GetMapping({"/reports/saved/{id:\\d+}", "/admin/reports/{id:\\d+}"})
    public String view(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Report report;
        try {
            report = savedReportService.require(id);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/reports/saved";
        }
        model.addAttribute("report", report);
        model.addAttribute("tickets", savedReportService.ticketsFor(report));
        return "report/saved-report";
    }

    @PostMapping({"/reports/saved/{id:\\d+}/rename", "/admin/reports/{id:\\d+}/rename"})
    public String rename(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam String name,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User actor = authService.requireByEmail(authentication.getName());
            Report report = savedReportService.updateName(actor, id, name);
            auditService.log(actor, "Renamed report to " + report.getName(), "REPORT", id);
            redirectAttributes.addFlashAttribute("success", "Report renamed successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reports/saved/" + id;
    }

    @PostMapping("/reports/saved/{id:\\d+}/delete")
    public String delete(
            Authentication authentication,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        return deleteAndRedirect(authentication, id, redirectAttributes, "/reports/saved");
    }

    @PostMapping("/admin/reports/{id:\\d+}/delete")
    public String deleteFromAdmin(
            Authentication authentication,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        return deleteAndRedirect(authentication, id, redirectAttributes, "/admin/reports");
    }

    private String deleteAndRedirect(
            Authentication authentication,
            Long id,
            RedirectAttributes redirectAttributes,
            String redirectPath
    ) {
        try {
            User actor = authService.requireByEmail(authentication.getName());
            Report report = savedReportService.require(id);
            String name = report.getName();
            savedReportService.delete(actor, id);
            auditService.log(actor, "Deleted report " + name, "REPORT", id);
            redirectAttributes.addFlashAttribute("success", "Report deleted.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:" + redirectPath;
    }

    @GetMapping({"/reports/saved/{id:\\d+}/edit", "/admin/reports/{id:\\d+}/edit"})
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Report report;
        try {
            report = savedReportService.require(id);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/reports/saved";
        }
        if (!model.containsAttribute("reportForm")) {
            SavedReportForm form = new SavedReportForm();
            form.setName(report.getName());
            form.setDepartment(report.getDepartment());
            form.setStatus(report.getStatus());
            form.setPriority(report.getPriority());
            model.addAttribute("reportForm", form);
        }
        model.addAttribute("report", report);
        model.addAttribute("departments", savedReportService.departments());
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("priorities", TicketPriority.values());
        return "report/saved-report-edit";
    }

    @PostMapping({"/reports/saved/{id:\\d+}", "/admin/reports/{id:\\d+}"})
    public String update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @ModelAttribute("reportForm") SavedReportForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("report", savedReportService.require(id));
            model.addAttribute("departments", savedReportService.departments());
            model.addAttribute("statuses", TicketStatus.values());
            model.addAttribute("priorities", TicketPriority.values());
            return "report/saved-report-edit";
        }
        try {
            User actor = authService.requireByEmail(authentication.getName());
            savedReportService.update(actor, id, form.getName(), form.getDepartment(), form.getStatus(), form.getPriority());
            auditService.log(actor, "Updated report " + form.getName(), "REPORT", id);
            redirectAttributes.addFlashAttribute("success", "Report updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reports/saved/" + id;
    }
}
