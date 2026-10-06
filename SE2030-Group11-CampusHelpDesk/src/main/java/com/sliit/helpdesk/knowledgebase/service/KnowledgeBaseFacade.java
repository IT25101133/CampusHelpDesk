package com.sliit.helpdesk.knowledgebase.service;

import com.sliit.helpdesk.knowledgebase.model.KbArticle;
import com.sliit.helpdesk.knowledgebase.model.KbCategory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Facade for the knowledge base.
 * Pages and the API ask this one object for articles and categories
 * instead of calling each service themselves.
 */
@Service
public class KnowledgeBaseFacade {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseFacade(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    public Browse open(String query) {
        return new Browse(knowledgeBaseService.articles(query), knowledgeBaseService.categories());
    }

    public KbArticle openArticle(Long id) {
        return knowledgeBaseService.view(id);
    }

    public List<KbCategory> categories() {
        return knowledgeBaseService.categories();
    }

    public record Browse(List<KbArticle> articles, List<KbCategory> categories) {
    }
}
