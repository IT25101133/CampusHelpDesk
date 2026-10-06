package com.sliit.helpdesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HelpDeskApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void spaIndexIsPublic() throws Exception {
        mockMvc.perform(get("/index.html")).andExpect(status().isOk());
    }

    @Test
    void studentCanLoadTicketsNotificationsAndKb() throws Exception {
        String token = login("student@sliit.lk");

        mockMvc.perform(get("/api/tickets").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists());

        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/kb").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists());
    }

    @Test
    void deptHeadCanLoadReports() throws Exception {
        String token = login("head@sliit.lk");

        mockMvc.perform(get("/api/reports").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.totalTickets").exists());
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
