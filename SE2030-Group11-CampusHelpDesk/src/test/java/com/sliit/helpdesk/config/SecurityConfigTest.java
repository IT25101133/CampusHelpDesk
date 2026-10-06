package com.sliit.helpdesk.config;

// Security Config Test is part of the campus help desk config code.

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authEndpointsArePublic() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@sliit.lk\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otherApiEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminApiRequiresToken() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRoleCanAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DEPT_HEAD")
    void deptHeadCannotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "head@sliit.lk", roles = "DEPT_HEAD")
    void deptHeadCanAccessReportsApi() throws Exception {
        mockMvc.perform(get("/api/reports/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DEPT_HEAD")
    void deptHeadCanAccessSummaryReport() throws Exception {
        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DEPT_HEAD")
    void deptHeadCannotAccessAuditLogs() throws Exception {
        mockMvc.perform(get("/api/reports/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAuditLogs() throws Exception {
        mockMvc.perform(get("/api/reports/audit-logs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessReportsApi() throws Exception {
        mockMvc.perform(get("/api/reports/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void categoryReadIsOpenWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotCreateCategory() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Parking\",\"department\":\"Facilities\",\"slaHours\":24}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateCategory() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Security Probe Category\",\"department\":\"Library\",\"slaHours\":36}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "student@sliit.lk", roles = "STUDENT")
    void studentCanBrowseKnowledgeBaseArticles() throws Exception {
        mockMvc.perform(get("/api/kb/articles").param("search", "password"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "student@sliit.lk", roles = "STUDENT")
    void studentCannotCreateKnowledgeBaseArticle() throws Exception {
        mockMvc.perform(post("/api/kb/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"WiFi help\",\"content\":\"Restart the campus access point.\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "staff@sliit.lk", roles = "STAFF")
    void staffCanCreateKnowledgeBaseArticle() throws Exception {
        mockMvc.perform(post("/api/kb/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Connect to campus WiFi\",\"content\":\"Use eduroam with your student email.\"}"))
                .andExpect(status().isCreated());
    }
}
