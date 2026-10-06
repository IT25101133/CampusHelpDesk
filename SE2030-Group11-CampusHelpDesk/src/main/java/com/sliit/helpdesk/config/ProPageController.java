package com.sliit.helpdesk.config;

// Pro Page Controller is part of the campus help desk config code.

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProPageController {

    @GetMapping("/welcome")
    public String welcome() {
        return "forward:/login.html";
    }

    @GetMapping("/workspace")
    public String workspace() {
        return "forward:/dashboard.html";
    }

    @GetMapping("/ledger")
    public String ledger() {
        return "forward:/tickets.html";
    }

    @GetMapping("/request")
    public String request() {
        return "forward:/submit.html";
    }

    @GetMapping("/kb")
    public String knowledge() {
        return "forward:/knowledge.html";
    }

    @GetMapping("/register")
    public String register() {
        return "forward:/register.html";
    }

    @GetMapping("/accounts")
    public String accounts() {
        return "forward:/accounts.html";
    }

    @GetMapping("/settings")
    public String profile() {
        return "forward:/profile.html";
    }

    @GetMapping("/inbox")
    public String inbox() {
        return "forward:/notifications.html";
    }

    @GetMapping("/insights")
    public String reports() {
        return "forward:/reports.html";
    }

    @GetMapping("/departments")
    public String categories() {
        return "forward:/categories.html";
    }

    @GetMapping("/reset")
    public String reset() {
        return "forward:/reset.html";
    }
}
