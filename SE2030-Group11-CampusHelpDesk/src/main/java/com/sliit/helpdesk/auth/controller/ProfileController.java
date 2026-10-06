package com.sliit.helpdesk.auth.controller;

// Profile Controller is part of the campus help desk controller code.

import com.sliit.helpdesk.auth.dto.ProfileUpdateRequest;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AdminAccountProtectedException;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.auth.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final AuthService authService;
    private final UserService userService;

    public ProfileController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        User user = authService.requireByEmail(authentication.getName());
        if (!model.containsAttribute("profileForm")) {
            ProfileUpdateRequest form = new ProfileUpdateRequest();
            form.setFullName(user.getFullName());
            form.setDepartment(user.getDepartment());
            form.setStudentId(user.getStudentId());
            model.addAttribute("profileForm", form);
        }
        model.addAttribute("user", user);
        return "auth/profile";
    }

    @PostMapping("/profile")
    public String update(
            Authentication authentication,
            @Valid @ModelAttribute("profileForm") ProfileUpdateRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User user = authService.requireByEmail(authentication.getName());
        model.addAttribute("user", user);
        if (bindingResult.hasErrors()) {
            return "auth/profile";
        }
        try {
            authService.updateProfile(user, form);
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("newPassword", "weak", ex.getMessage());
            return "auth/profile";
        }
        redirectAttributes.addFlashAttribute("success", "Profile updated.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/deactivate")
    public String deactivate(
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) throws ServletException {
        User user = authService.requireByEmail(authentication.getName());
        try {
            userService.deactivateAccount(user, user.getId());
        } catch (AdminAccountProtectedException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/profile";
        }
        request.logout();
        return "redirect:/login?deactivated";
    }
}
