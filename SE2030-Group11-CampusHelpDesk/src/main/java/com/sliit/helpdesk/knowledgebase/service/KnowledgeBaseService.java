package com.sliit.helpdesk.knowledgebase.service;

// Knowledge Base Service is part of the campus help desk service code.

import com.sliit.helpdesk.knowledgebase.model.KbArticle;
import com.sliit.helpdesk.knowledgebase.model.KbCategory;
import com.sliit.helpdesk.knowledgebase.repository.ArticleRepository;
import com.sliit.helpdesk.knowledgebase.repository.KbCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeBaseService {

    private final ArticleRepository articleRepository;
    private final KbCategoryRepository categoryRepository;

    public KnowledgeBaseService(ArticleRepository articleRepository, KbCategoryRepository categoryRepository) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<KbArticle> articles(String query) {
        if (query == null || query.isBlank()) {
            return articleRepository.findAllByOrderByCreatedAtDesc();
        }
        return articleRepository.search(query.trim());
    }

    public List<KbCategory> categories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public KbArticle view(Long id) {
        KbArticle article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));
        article.setViewCount(article.getViewCount() + 1);
        return articleRepository.save(article);
    }
}
