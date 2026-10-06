package com.sliit.helpdesk.category.controller;

import com.sliit.helpdesk.category.dto.CategoryForm;
import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.category.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public String list(Model model) {
        model.addAttribute("categories", categoryService.allCategories());
        return "category/list";
    }

    @GetMapping("/categories/new")
    public String createForm(Model model) {
        model.addAttribute("categoryForm", new CategoryForm());
        return "category/form";
    }

    @GetMapping("/categories/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Category category = categoryService.require(id);
        CategoryForm form = new CategoryForm();
        form.setName(category.getName());
        form.setDepartment(category.getDepartment());
        form.setSlaHours(category.getSlaHours());
        model.addAttribute("categoryForm", form);
        model.addAttribute("categoryId", id);
        return "category/form";
    }

    @PostMapping("/categories")
    public String create(
            @Valid @ModelAttribute("categoryForm") CategoryForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        return persist(null, form, bindingResult, model, redirectAttributes);
    }

    @PostMapping("/categories/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("categoryForm") CategoryForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        return persist(id, form, bindingResult, model, redirectAttributes);
    }

    private String persist(
            Long id,
            CategoryForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categoryId", id);
            return "category/form";
        }
        try {
            categoryService.save(form, id);
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            model.addAttribute("categoryId", id);
            return "category/form";
        }
        redirectAttributes.addFlashAttribute("success", "Category saved.");
        return "redirect:/categories";
    }
}
