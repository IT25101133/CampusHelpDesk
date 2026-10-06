package com.sliit.helpdesk.knowledgebase.service;

// Kb Category Service is part of the campus help desk service code.

import com.sliit.helpdesk.knowledgebase.dto.KbCategoryRequest;
import com.sliit.helpdesk.knowledgebase.model.KbCategory;
import com.sliit.helpdesk.knowledgebase.repository.ArticleRepository;
import com.sliit.helpdesk.knowledgebase.repository.KbCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KbCategoryService {

    private final KbCategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;

    public KbCategoryService(KbCategoryRepository categoryRepository, ArticleRepository articleRepository) {
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
    }

    public List<KbCategory> list() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public KbCategory require(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    @Transactional
    public KbCategory create(KbCategoryRequest request) {
        return save(new KbCategory(), request);
    }

    @Transactional
    public KbCategory update(Long id, KbCategoryRequest request) {
        return save(require(id), request);
    }

    @Transactional
    public void delete(Long id) {
        KbCategory category = require(id);
        if (articleRepository.countByCategory_Id(id) > 0) {
            throw new IllegalArgumentException("Category still has existing articles.");
        }
        categoryRepository.delete(category);
    }

    private KbCategory save(KbCategory category, KbCategoryRequest request) {
        String name = request.getName().trim();
        boolean taken = categoryRepository.existsByNameIgnoreCase(name);
        if (taken && (category.getId() == null || !name.equalsIgnoreCase(category.getName()))) {
            throw new IllegalArgumentException("A category with that name already exists.");
        }
        category.setName(name);
        return categoryRepository.save(category);
    }
}
