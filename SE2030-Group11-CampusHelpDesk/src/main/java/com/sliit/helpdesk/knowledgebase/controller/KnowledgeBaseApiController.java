package com.sliit.helpdesk.knowledgebase.controller;

import com.sliit.helpdesk.knowledgebase.dto.KbArticleResponse;
import com.sliit.helpdesk.knowledgebase.service.KnowledgeBaseService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/kb")
public class KnowledgeBaseApiController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseApiController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<KbArticleResponse> list(@RequestParam(value = "q", required = false) String query) {
        return knowledgeBaseService.articles(query).stream().map(KbArticleResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Transactional
    public KbArticleResponse article(@PathVariable Long id) {
        return KbArticleResponse.from(knowledgeBaseService.view(id));
    }
}
