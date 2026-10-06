package com.sliit.helpdesk.knowledgebase.controller;

import com.sliit.helpdesk.knowledgebase.service.KnowledgeBaseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping("/knowledgebase")
    public String index(@RequestParam(value = "q", required = false) String query, Model model) {
        model.addAttribute("articles", knowledgeBaseService.articles(query));
        model.addAttribute("kbCategories", knowledgeBaseService.categories());
        model.addAttribute("q", query == null ? "" : query);
        return "knowledgebase/index";
    }

    @GetMapping("/knowledgebase/{id}")
    public String article(@PathVariable Long id, Model model) {
        model.addAttribute("article", knowledgeBaseService.view(id));
        return "knowledgebase/article";
    }
}
