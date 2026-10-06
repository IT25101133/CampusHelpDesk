package com.sliit.helpdesk.knowledgebase.controller;

// Knowledge Base Controller is part of the campus help desk controller code.

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.knowledgebase.dto.ArticleRequest;
import com.sliit.helpdesk.knowledgebase.dto.KbCategoryRequest;
import com.sliit.helpdesk.knowledgebase.model.KbArticle;
import com.sliit.helpdesk.knowledgebase.service.ArticleService;
import com.sliit.helpdesk.knowledgebase.service.KbCategoryService;
import com.sliit.helpdesk.knowledgebase.service.KnowledgeBaseFacade;
import com.sliit.helpdesk.knowledgebase.service.KnowledgeBaseService;
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
public class KnowledgeBaseController {

    private final KnowledgeBaseFacade knowledgeBaseFacade;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ArticleService articleService;
    private final KbCategoryService kbCategoryService;
    private final AuthService authService;

    public KnowledgeBaseController(
            KnowledgeBaseFacade knowledgeBaseFacade,
            KnowledgeBaseService knowledgeBaseService,
            ArticleService articleService,
            KbCategoryService kbCategoryService,
            AuthService authService
    ) {
        this.knowledgeBaseFacade = knowledgeBaseFacade;
        this.knowledgeBaseService = knowledgeBaseService;
        this.articleService = articleService;
        this.kbCategoryService = kbCategoryService;
        this.authService = authService;
    }

    @GetMapping("/knowledgebase")
    public String index(@RequestParam(value = "q", required = false) String query, Model model) {
        KnowledgeBaseFacade.Browse browse = knowledgeBaseFacade.open(query);
        model.addAttribute("articles", browse.articles());
        model.addAttribute("kbCategories", browse.categories());
        model.addAttribute("q", query == null ? "" : query);
        model.addAttribute("categoryForm", new KbCategoryRequest());
        return "knowledgebase/index";
    }

    @GetMapping("/knowledgebase/new")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public String createForm(Model model) {
        if (!model.containsAttribute("articleForm")) {
            model.addAttribute("articleForm", new ArticleRequest());
        }
        model.addAttribute("kbCategories", knowledgeBaseService.categories());
        return "knowledgebase/form";
    }

    @GetMapping("/knowledgebase/{id}")
    public String article(@PathVariable Long id, Model model) {
        model.addAttribute("article", knowledgeBaseFacade.openArticle(id));
        return "knowledgebase/article";
    }

    @GetMapping("/knowledgebase/{id}/edit")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        KbArticle article = articleService.require(id);
        if (!model.containsAttribute("articleForm")) {
            ArticleRequest form = new ArticleRequest();
            form.setTitle(article.getTitle());
            form.setContent(article.getContent());
            form.setCategoryId(article.getCategory() == null ? null : article.getCategory().getId());
            model.addAttribute("articleForm", form);
        }
        model.addAttribute("articleId", id);
        model.addAttribute("kbCategories", knowledgeBaseService.categories());
        return "knowledgebase/form";
    }

    @PostMapping("/knowledgebase")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public String create(
            Authentication authentication,
            @Valid @ModelAttribute("articleForm") ArticleRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("kbCategories", knowledgeBaseService.categories());
            return "knowledgebase/form";
        }
        User author = authService.requireByEmail(authentication.getName());
        KbArticle saved = articleService.create(author, form);
        redirectAttributes.addFlashAttribute("success", "Article published.");
        return "redirect:/knowledgebase/" + saved.getId();
    }

    @PostMapping("/knowledgebase/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("articleForm") ArticleRequest form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("articleId", id);
            model.addAttribute("kbCategories", knowledgeBaseService.categories());
            return "knowledgebase/form";
        }
        articleService.update(id, form);
        redirectAttributes.addFlashAttribute("success", "Article updated.");
        return "redirect:/knowledgebase/" + id;
    }

    @PostMapping("/knowledgebase/{id}/delete")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public String deleteArticle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        articleService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Article deleted.");
        return "redirect:/knowledgebase";
    }

    @PostMapping("/knowledgebase/categories")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public String createCategory(
            @Valid @ModelAttribute("categoryForm") KbCategoryRequest form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Category name is required.");
            return "redirect:/knowledgebase";
        }
        try {
            kbCategoryService.create(form);
            redirectAttributes.addFlashAttribute("success", "Knowledge base category created.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/knowledgebase";
    }

    @PostMapping("/knowledgebase/categories/{id}/delete")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            kbCategoryService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Knowledge base category deleted.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/knowledgebase";
    }
}
