package com.sliit.helpdesk.report;

// Saved Report Controller Test is part of the campus help desk report code.

import com.sliit.helpdesk.report.model.Report;
import com.sliit.helpdesk.report.repository.ReportRepository;
import com.sliit.helpdesk.ticket.model.TicketStatus;
import com.sliit.helpdesk.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class SavedReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    @WithMockUser(username = "admin@sliit.lk", roles = "ADMIN")
    void adminCanOpenSavedReportsPage() throws Exception {
        mockMvc.perform(get("/reports/saved"))
                .andExpect(status().isOk())
                .andExpect(view().name("report/saved-reports"))
                .andExpect(model().attributeExists("reports", "departments", "statuses", "priorities"));
    }

    @Test
    @WithMockUser(username = "staff@sliit.lk", roles = "STAFF")
    void staffCannotOpenSavedReportsPage() throws Exception {
        mockMvc.perform(get("/reports/saved"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@sliit.lk", roles = "ADMIN")
    void generateStoresReportAndDetailPageRenders() throws Exception {
        mockMvc.perform(post("/reports/saved/generate").with(csrf())
                        .param("name", "Resolved this month")
                        .param("department", "")
                        .param("status", "RESOLVED")
                        .param("priority", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reports/saved"));

        Report saved = latest("Resolved this month");
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(saved.getDepartment()).isNull();
        assertThat(saved.getPriority()).isNull();
        assertThat(saved.getCreatedBy()).isNotNull();
        // The stored figure has to agree with the metric the dashboard reports.
        assertThat(saved.getTicketCount()).isEqualTo(ticketRepository.countByStatus(TicketStatus.RESOLVED));
        assertThat(saved.getTicketCount()).isPositive();

        mockMvc.perform(get("/reports/saved/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("report/saved-report"))
                .andExpect(model().attributeExists("report", "tickets"));
    }

    @Test
    @WithMockUser(username = "admin@sliit.lk", roles = "ADMIN")
    void generateRejectsBlankName() throws Exception {
        mockMvc.perform(post("/reports/saved/generate").with(csrf())
                        .param("name", "  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reports/saved"));

        assertThat(reportRepository.findAllByOrderByCreatedAtDesc())
                .noneMatch(report -> report.getName().isBlank());
    }

    @Test
    @WithMockUser(username = "admin@sliit.lk", roles = "ADMIN")
    void deleteRemovesOnlyTheTargetedReport() throws Exception {
        mockMvc.perform(post("/reports/saved/generate").with(csrf()).param("name", "Keep me"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/reports/saved/generate").with(csrf()).param("name", "Delete me"))
                .andExpect(status().is3xxRedirection());

        Report doomed = latest("Delete me");

        mockMvc.perform(post("/reports/saved/{id}/delete", doomed.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reports/saved"));

        List<Report> remaining = reportRepository.findAllByOrderByCreatedAtDesc();
        assertThat(remaining).noneMatch(report -> report.getId().equals(doomed.getId()));
        assertThat(remaining).anyMatch(report -> "Keep me".equals(report.getName()));
    }

    @Test
    @WithMockUser(username = "admin@sliit.lk", roles = "ADMIN")
    void adminCanRenameSavedReport() throws Exception {
        mockMvc.perform(post("/reports/saved/generate").with(csrf()).param("name", "Original Name"))
                .andExpect(status().is3xxRedirection());

        Report report = latest("Original Name");

        mockMvc.perform(post("/reports/saved/{id}/rename", report.getId()).with(csrf())
                        .param("name", "Renamed Title"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reports/saved/" + report.getId()));

        Report updated = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Renamed Title");
    }

    @Test
    @WithMockUser(username = "staff@sliit.lk", roles = "STAFF")
    void staffCannotDeleteReports() throws Exception {
        mockMvc.perform(post("/reports/saved/1/delete").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@sliit.lk", roles = "ADMIN")
    void adminDeleteEndpointRemovesOnlyTheTargetedReport() throws Exception {
        mockMvc.perform(post("/admin/reports/generate").with(csrf()).param("name", "Keep via admin"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/reports/generate").with(csrf()).param("name", "Delete via admin"))
                .andExpect(status().is3xxRedirection());

        Report doomed = latest("Delete via admin");

        mockMvc.perform(post("/admin/reports/{id}/delete", doomed.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reports"));

        List<Report> remaining = reportRepository.findAllByOrderByCreatedAtDesc();
        assertThat(remaining).noneMatch(report -> report.getId().equals(doomed.getId()));
        assertThat(remaining).anyMatch(report -> "Keep via admin".equals(report.getName()));
    }

    @Test
    @WithMockUser(username = "staff@sliit.lk", roles = "STAFF")
    void staffCannotDeleteViaAdminReportsEndpoint() throws Exception {
        mockMvc.perform(post("/admin/reports/1/delete").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@sliit.lk", roles = "ADMIN")
    void dashboardIncludesResolutionStats() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("report/dashboard"))
                .andExpect(model().attributeExists("stats", "recentTickets", "resolutionStats"));

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("report/dashboard"))
                .andExpect(model().attributeExists("resolutionStats"));
    }

    @Test
    @WithMockUser(username = "staff@sliit.lk", roles = "STAFF")
    void staffCannotOpenAdminDashboardAlias() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    private Report latest(String name) {
        return reportRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(report -> name.equals(report.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Report '" + name + "' was not saved"));
    }
}
