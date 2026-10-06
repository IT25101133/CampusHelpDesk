package com.sliit.helpdesk.auth.controller;

// User Web Controller is part of the campus help desk controller code.

import com.sliit.helpdesk.auth.dto.AdminUserRequest;
import com.sliit.helpdesk.auth.model.Role;
import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.UserService;
import jakarta.validation.Valid;
import com.sliit.helpdesk.auth.service.AdminAccountProtectedException;
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
@PreAuthorize("hasRole('ADMIN')")
public class UserWebController {

    private final UserService userService;

    public UserWebController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public String list(Model model) {
        model.addAttribute("users", userService.listAll());
        return "auth/users";
    }

    @GetMapping("/users/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("userForm")) {
            model.addAttribute("userForm", new AdminUserRequest());
        }
        model.addAttribute("roles", Role.values());
        return "auth/user-form";
    }

    @GetMapping("/users/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User user = userService.requireById(id);
        if (!model.containsAttribute("userForm")) {
            AdminUserRequest form = new AdminUserRequest();
            form.setFullName(user.getFullName());
            form.setEmail(user.getEmail());
            form.setRole(user.getRole());
            form.setDepartment(user.getDepartment());
            form.setStudentId(user.getStudentId());
            form.setEnabled(user.isEnabled());
            model.addAttribute("userForm", form);
        }
        model.addAttribute("userId", id);
        model.addAttribute("roles", Role.values());
        return "auth/user-form";
    }

    @PostMapping("/users")
    public String create(
            @Valid @ModelAttribute("userForm") AdminUserRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "auth/user-form";
        }
        try {
            userService.create(form);
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("email", "duplicate", ex.getMessage());
            model.addAttribute("roles", Role.values());
            return "auth/user-form";
        }
        redirectAttributes.addFlashAttribute("success", "User created.");
        return "redirect:/users";
    }

    @PostMapping("/users/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("userForm") AdminUserRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("userId", id);
            model.addAttribute("roles", Role.values());
            return "auth/user-form";
        }
        try {
            userService.update(id, form);
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("email", "duplicate", ex.getMessage());
            model.addAttribute("userId", id);
            model.addAttribute("roles", Role.values());
            return "auth/user-form";
        }
        redirectAttributes.addFlashAttribute("success", "User updated.");
        return "redirect:/users";
    }

    @PostMapping("/users/{id}/delete")
    public String delete(
            @PathVariable Long id,
            @RequestParam(name = "permanent", defaultValue = "false") boolean permanent,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User actor = userService.requireByEmail(authentication.getName());
            userService.deleteAccount(actor, id, permanent);
            redirectAttributes.addFlashAttribute("success",
                    permanent ? "Account permanently deleted." : "User deactivated.");
        } catch (AdminAccountProtectedException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/users";
    }
}
