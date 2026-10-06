package com.sliit.helpdesk.knowledgebase.controller;

// Knowledge Base Api Controller is part of the campus help desk controller code.

import com.sliit.helpdesk.knowledgebase.dto.KbArticleResponse;
import com.sliit.helpdesk.knowledgebase.service.KnowledgeBaseFacade;
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

    private final KnowledgeBaseFacade knowledgeBaseFacade;

    public KnowledgeBaseApiController(KnowledgeBaseFacade knowledgeBaseFacade) {
        this.knowledgeBaseFacade = knowledgeBaseFacade;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<KbArticleResponse> list(@RequestParam(value = "q", required = false) String query) {
        return knowledgeBaseFacade.open(query).articles().stream().map(KbArticleResponse::from).toList();
    }

    @GetMapping("/{id:\\d+}")
    @Transactional
    public KbArticleResponse article(@PathVariable Long id) {
        return KbArticleResponse.from(knowledgeBaseFacade.openArticle(id));
    }
}
