package com.sliit.helpdesk.auth.controller;

// Auth Controller is part of the campus help desk controller code.

import com.sliit.helpdesk.auth.dto.RegisterRequest;
import com.sliit.helpdesk.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            authService.register(request);
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("email", "exists", ex.getMessage());
            return "auth/register";
        }
        redirectAttributes.addFlashAttribute("success", "Account created. Sign in to continue.");
        return "redirect:/login";
    }
}
