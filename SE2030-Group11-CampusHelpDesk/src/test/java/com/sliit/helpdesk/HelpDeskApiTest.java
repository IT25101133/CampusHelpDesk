package com.sliit.helpdesk;

// Help Desk Api Test is part of the campus help desk helpdesk code.

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HelpDeskApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testAccountCanSignIn() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@sliit.lk\",\"password\":\"Test1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void publicRegisterCreatesStudentsOnly() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String studentEmail = "new.student." + suffix + "@my.sliit.lk";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"New Student\",\"email\":\"" + studentEmail
                                + "\",\"password\":\"password1\",\"role\":\"STUDENT\",\"department\":\"Faculty of Computing\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"New Lecturer\",\"email\":\"new.lecturer." + suffix
                                + "@sliit.lk\",\"password\":\"password1\",\"role\":\"LECTURER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only students can create an account from the sign-in page."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"New Admin\",\"email\":\"new.admin." + suffix
                                + "@sliit.lk\",\"password\":\"password1\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void spaIndexIsPublic() throws Exception {
        mockMvc.perform(get("/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
        mockMvc.perform(get("/register.html")).andExpect(status().isOk());
        mockMvc.perform(get("/settings")).andExpect(status().isOk());
        mockMvc.perform(get("/inbox")).andExpect(status().isOk());
        mockMvc.perform(get("/insights")).andExpect(status().isOk());
        mockMvc.perform(get("/departments")).andExpect(status().isOk());
        mockMvc.perform(get("/reset")).andExpect(status().isOk());
    }

    @Test
    void studentCanViewProfileAndRequestPasswordReset() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@sliit.lk\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").isNotEmpty())
                .andExpect(jsonPath("$.resetPath").exists());

        String token = login("student@sliit.lk");
        mockMvc.perform(get("/api/auth/profile").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@sliit.lk"));
        mockMvc.perform(get("/api/auth/activity").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tickets").param("q", "wifi").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void studentCanLoadTicketsNotificationsAndKb() throws Exception {
        String token = login("student@sliit.lk");

        mockMvc.perform(get("/api/tickets").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists());

        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/kb/articles").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists());

        mockMvc.perform(get("/api/kb/articles").param("search", "password").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Reset your campus password')]").exists());

        mockMvc.perform(get("/api/kb/suggest").param("query", "classroom projector broken").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Report a classroom fault"));
    }

    @Test
    void deptHeadCanLoadReports() throws Exception {
        String token = login("head@sliit.lk");

        mockMvc.perform(get("/api/reports").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.totalTickets").exists());

        mockMvc.perform(get("/api/reports/summary").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").exists())
                .andExpect(jsonPath("$.openTickets").exists())
                .andExpect(jsonPath("$.pendingTickets").exists())
                .andExpect(jsonPath("$.resolvedTickets").exists());

        mockMvc.perform(get("/api/reports/resolution-time-avg").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageHours").exists());

        mockMvc.perform(get("/api/reports/overdue").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/audit-logs").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiErrorsReturnJsonInsteadOfStackTrace() throws Exception {
        String token = login("student@sliit.lk");

        mockMvc.perform(get("/api/tickets/99999").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Ticket not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/tickets/99999"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@sliit.lk\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void adminCanExportSummaryAndViewAuditLogs() throws Exception {
        String token = login("admin@sliit.lk");

        mockMvc.perform(get("/api/reports/audit-logs").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").exists());

        mockMvc.perform(get("/api/reports/export").param("format", "excel").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Category is required."));

        long categoryId = categoryIdNamed(token, "IT Support");
        long assigneeId = userId("it.support@sliit.lk");
        long creatorId = userId("student@sliit.lk");
        mockMvc.perform(get("/api/reports/export")
                        .param("format", "excel")
                        .param("categoryId", String.valueOf(categoryId))
                        .param("department", "IT Services")
                        .param("status", "OPEN")
                        .param("priority", "HIGH")
                        .param("from", "2026-01-01")
                        .param("to", "2026-10-06")
                        .param("assignedTo", String.valueOf(assigneeId))
                        .param("createdBy", String.valueOf(creatorId))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/export")
                        .param("format", "pdf")
                        .param("categoryId", String.valueOf(categoryId))
                        .param("department", "IT Services")
                        .param("status", "OPEN")
                        .param("priority", "HIGH")
                        .param("from", "2026-01-01")
                        .param("to", "2026-10-06")
                        .param("assignedTo", String.valueOf(assigneeId))
                        .param("createdBy", String.valueOf(creatorId))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void ticketSubmitRequiresDepartmentAndPersistsIt() throws Exception {
        String token = login("student@sliit.lk");

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content("{\"title\":\"Wi-Fi down in library\",\"description\":\"No signal on floor 2\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Please select a department"));

        MvcResult options = mockMvc.perform(get("/api/tickets/options").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        String optionsJson = options.getResponse().getContentAsString();
        int nameAt = optionsJson.indexOf("\"name\":\"IT Support\"");
        org.assertj.core.api.Assertions.assertThat(nameAt).isGreaterThanOrEqualTo(0);
        int idKey = optionsJson.lastIndexOf("\"id\":", nameAt);
        String idChunk = optionsJson.substring(idKey + 5, optionsJson.indexOf(',', idKey));
        long categoryId = Long.parseLong(idChunk.trim());

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content("{\"title\":\"Wi-Fi down in library\",\"description\":\"No signal on floor 2 of the library.\",\"priority\":\"HIGH\",\"categoryId\":"
                                + categoryId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("IT Support"))
                .andExpect(jsonPath("$.department").value("IT Services"))
                .andExpect(jsonPath("$.categoryId").value(categoryId));
    }

    @Test
    void ticketSubmitAcceptsCaseInsensitivePriority() throws Exception {
        String token = login("student@sliit.lk");

        MvcResult options = mockMvc.perform(get("/api/tickets/options").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        String optionsJson = options.getResponse().getContentAsString();
        int nameAt = optionsJson.indexOf("\"name\":\"IT Support\"");
        org.assertj.core.api.Assertions.assertThat(nameAt).isGreaterThanOrEqualTo(0);
        int idKey = optionsJson.lastIndexOf("\"id\":", nameAt);
        String idChunk = optionsJson.substring(idKey + 5, optionsJson.indexOf(',', idKey));
        long categoryId = Long.parseLong(idChunk.trim());

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content("{\"title\":\"Case insensitive priority ticket\",\"description\":\"Detailed explanation\",\"priority\":\"high\",\"categoryId\":"
                                + categoryId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("HIGH"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content("{\"title\":\"Critical priority ticket\",\"description\":\"Server room power failure\",\"priority\":\"crit\",\"categoryId\":"
                                + categoryId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("CRITICAL"));
    }

    @Test
    void ticketSubmitWithAttachmentSucceeds() throws Exception {
        String token = login("student@sliit.lk");

        MvcResult options = mockMvc.perform(get("/api/tickets/options").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        String optionsJson = options.getResponse().getContentAsString();
        int nameAt = optionsJson.indexOf("\"name\":\"IT Support\"");
        int idKey = optionsJson.lastIndexOf("\"id\":", nameAt);
        String idChunk = optionsJson.substring(idKey + 5, optionsJson.indexOf(',', idKey));
        long categoryId = Long.parseLong(idChunk.trim());

        MvcResult ticketResult = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(token))
                        .content("{\"title\":\"Ticket with Attachment\",\"description\":\"See attached screenshot\",\"priority\":\"HIGH\",\"categoryId\":"
                                + categoryId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        long ticketId = jsonId(ticketResult);

        org.springframework.mock.web.MockMultipartFile mockFile = new org.springframework.mock.web.MockMultipartFile(
                "file",
                "screenshot.png",
                "image/png",
                "test-image-content".getBytes()
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/tickets/" + ticketId + "/attachments")
                        .file(mockFile)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("screenshot.png"))
                .andExpect(jsonPath("$.filePath").isNotEmpty());
    }

    @Test
    void eachMemberModuleHasCrud() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String adminToken = login("admin@sliit.lk");
        String staffToken = login("staff@sliit.lk");
        String studentToken = login("student@sliit.lk");

        String email = "crud." + suffix + "@sliit.lk";
        MvcResult userCreated = mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content("{\"fullName\":\"CRUD User\",\"email\":\"" + email
                                + "\",\"password\":\"password1\",\"role\":\"STUDENT\",\"department\":\"IT Services\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andReturn();
        long userId = jsonId(userCreated);
        mockMvc.perform(get("/api/admin/users/" + userId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("CRUD User"));
        mockMvc.perform(put("/api/admin/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content("{\"fullName\":\"CRUD User Updated\",\"email\":\"" + email
                                + "\",\"role\":\"STUDENT\",\"department\":\"Faculty of Computing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("CRUD User Updated"));
        mockMvc.perform(delete("/api/admin/users/" + userId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        String categoryName = "CRUD Category " + suffix;
        MvcResult categoryCreated = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content("{\"name\":\"" + categoryName + "\",\"department\":\"CRUD Lab\",\"slaHours\":12}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(categoryName))
                .andReturn();
        long categoryId = jsonId(categoryCreated);
        mockMvc.perform(get("/api/categories/" + categoryId))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/categories/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content("{\"name\":\"" + categoryName + " Updated\",\"department\":\"CRUD Lab\",\"slaHours\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slaHours").value(8));

        long ticketCategoryId = categoryIdNamed(studentToken, "IT Support");
        MvcResult ticketCreated = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(studentToken))
                        .content("{\"title\":\"CRUD ticket " + suffix
                                + "\",\"description\":\"Need this for viva CRUD.\",\"priority\":\"LOW\",\"categoryId\":"
                                + ticketCategoryId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.submitterRole").value("STUDENT"))
                .andReturn();
        long ticketId = jsonId(ticketCreated);
        mockMvc.perform(get("/api/tickets/" + ticketId).header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/tickets/" + ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(studentToken))
                        .content("{\"title\":\"CRUD ticket updated\",\"description\":\"Updated for viva CRUD.\",\"priority\":\"MEDIUM\",\"categoryId\":"
                                + ticketCategoryId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("CRUD ticket updated"))
                .andExpect(jsonPath("$.description").value("Updated for viva CRUD."))
                .andExpect(jsonPath("$.category").value("IT Support"));
        mockMvc.perform(delete("/api/tickets/" + ticketId).header("Authorization", bearer(studentToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/categories/" + categoryId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        MvcResult notificationCreated = mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(studentToken))
                        .content("{\"message\":\"CRUD notification " + suffix + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long notificationId = jsonId(notificationCreated);
        mockMvc.perform(get("/api/notifications/" + notificationId).header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/notifications/" + notificationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(studentToken))
                        .content("{\"message\":\"Updated CRUD notification\",\"read\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
        mockMvc.perform(delete("/api/notifications/" + notificationId).header("Authorization", bearer(studentToken)))
                .andExpect(status().isNoContent());

        String kbName = "CRUD KB " + suffix;
        MvcResult kbCreated = mockMvc.perform(post("/api/kb/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(staffToken))
                        .content("{\"name\":\"" + kbName + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long kbCategoryId = jsonId(kbCreated);
        mockMvc.perform(get("/api/kb/categories/" + kbCategoryId).header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/kb/categories/" + kbCategoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(staffToken))
                        .content("{\"name\":\"" + kbName + " Updated\"}"))
                .andExpect(status().isOk());
        String articleTitle = "CRUD article " + suffix;
        MvcResult articleCreated = mockMvc.perform(post("/api/kb/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(staffToken))
                        .content("{\"title\":\"" + articleTitle
                                + "\",\"content\":\"How to demo CRUD for the viva.\",\"categoryId\":"
                                + kbCategoryId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        long articleId = jsonId(articleCreated);
        mockMvc.perform(get("/api/kb/articles/" + articleId).header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/kb/articles/" + articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(staffToken))
                        .content("{\"title\":\"" + articleTitle
                                + "\",\"content\":\"Updated article content.\",\"categoryId\":" + kbCategoryId + "}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/kb/articles/" + articleId).header("Authorization", bearer(staffToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/kb/categories/" + kbCategoryId).header("Authorization", bearer(staffToken)))
                .andExpect(status().isNoContent());

        MvcResult auditCreated = mockMvc.perform(post("/api/reports/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content("{\"action\":\"CRUD audit " + suffix + "\",\"entityType\":\"TICKET\",\"entityId\":1}"))
                .andExpect(status().isCreated())
                .andReturn();
        long auditId = jsonId(auditCreated);
        mockMvc.perform(get("/api/reports/audit-logs/" + auditId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/reports/audit-logs/" + auditId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content("{\"action\":\"Updated CRUD audit\",\"entityType\":\"TICKET\",\"entityId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("Updated CRUD audit"));
        mockMvc.perform(delete("/api/reports/audit-logs/" + auditId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void submittersSeeOnlyTheirOwnTickets() throws Exception {
        String studentBody = mockMvc.perform(get("/api/tickets").header("Authorization", bearer(login("student@sliit.lk"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(studentBody).contains("Cannot sign in to the LMS");
        org.assertj.core.api.Assertions.assertThat(studentBody).doesNotContain("Broken projector in Lab B-204");

        String lecturerBody = mockMvc.perform(get("/api/tickets").header("Authorization", bearer(login("lecturer@sliit.lk"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(lecturerBody).contains("Broken projector in Lab B-204");
        org.assertj.core.api.Assertions.assertThat(lecturerBody).doesNotContain("Cannot sign in to the LMS");

        String staffBody = mockMvc.perform(get("/api/tickets").header("Authorization", bearer(login("staff@sliit.lk"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(staffBody).contains("Cannot sign in to the LMS");
        org.assertj.core.api.Assertions.assertThat(staffBody).contains("Broken projector in Lab B-204");
        org.assertj.core.api.Assertions.assertThat(staffBody).contains("\"submitterRole\":\"STUDENT\"");
        org.assertj.core.api.Assertions.assertThat(staffBody).contains("\"submitterRole\":\"LECTURER\"");
    }

    private long categoryIdNamed(String token, String name) throws Exception {
        String json = mockMvc.perform(get("/api/tickets/options").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String needle = "\"name\":\"" + name + "\"";
        int nameAt = json.indexOf(needle);
        org.assertj.core.api.Assertions.assertThat(nameAt).isGreaterThanOrEqualTo(0);
        int idKey = json.lastIndexOf("\"id\":", nameAt);
        String idChunk = json.substring(idKey + 5, json.indexOf(',', idKey));
        return Long.parseLong(idChunk.trim());
    }

    private static long jsonId(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        int key = body.indexOf("\"id\":");
        org.assertj.core.api.Assertions.assertThat(key).isGreaterThanOrEqualTo(0);
        int start = key + 5;
        while (start < body.length() && !Character.isDigit(body.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < body.length() && Character.isDigit(body.charAt(end))) {
            end++;
        }
        return Long.parseLong(body.substring(start, end));
    }

    private long userId(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        int key = body.indexOf("\"userId\":");
        org.assertj.core.api.Assertions.assertThat(key).isGreaterThanOrEqualTo(0);
        int start = key + 9;
        while (start < body.length() && !Character.isDigit(body.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < body.length() && Character.isDigit(body.charAt(end))) {
            end++;
        }
        return Long.parseLong(body.substring(start, end));
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"token\":\"") + 9;
        int end = body.indexOf('"', start);
        return body.substring(start, end);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
