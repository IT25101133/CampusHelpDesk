package com.sliit.helpdesk.knowledgebase.service;

// Article Service is part of the campus help desk service code.

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.knowledgebase.dto.ArticleRequest;
import com.sliit.helpdesk.knowledgebase.model.KbArticle;
import com.sliit.helpdesk.knowledgebase.model.KbCategory;
import com.sliit.helpdesk.knowledgebase.repository.ArticleRepository;
import com.sliit.helpdesk.knowledgebase.repository.KbCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ArticleService {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
            "has", "have", "in", "is", "it", "of", "on", "or", "that", "the",
            "this", "to", "was", "with", "you", "your"
    );

    private final ArticleRepository articleRepository;
    private final KbCategoryRepository categoryRepository;

    public ArticleService(ArticleRepository articleRepository, KbCategoryRepository categoryRepository) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<KbArticle> search(String search, String category) {
        return articleRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(article -> matchesSearch(article, search))
                .filter(article -> matchesCategory(article, category))
                .toList();
    }

    public KbArticle require(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));
    }

    @Transactional
    public KbArticle view(Long id) {
        KbArticle article = require(id);
        article.setViewCount(article.getViewCount() + 1);
        return articleRepository.save(article);
    }

    public List<KbArticle> suggest(String query) {
        List<String> keywords = keywords(query);
        if (keywords.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<Long, KbArticle> matches = new LinkedHashMap<>();
        for (String keyword : keywords) {
            for (KbArticle article : articleRepository.search(keyword)) {
                matches.putIfAbsent(article.getId(), article);
            }
        }
        return new ArrayList<>(matches.values());
    }

    @Transactional
    public KbArticle create(User author, ArticleRequest request) {
        KbArticle article = new KbArticle();
        article.setAuthor(author);
        article.setViewCount(0);
        apply(article, request);
        return articleRepository.save(article);
    }

    @Transactional
    public KbArticle update(Long id, ArticleRequest request) {
        KbArticle article = require(id);
        apply(article, request);
        return articleRepository.save(article);
    }

    @Transactional
    public void delete(Long id) {
        articleRepository.delete(require(id));
    }

    private void apply(KbArticle article, ArticleRequest request) {
        article.setTitle(request.getTitle().trim());
        article.setContent(request.getContent().trim());
        article.setCategory(resolveCategory(request.getCategoryId()));
    }

    private KbCategory resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    static boolean matchesSearch(KbArticle article, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String needle = search.trim().toLowerCase(Locale.ROOT);
        return contains(article.getTitle(), needle) || contains(article.getContent(), needle);
    }

    static boolean matchesCategory(KbArticle article, String category) {
        if (category == null || category.isBlank()) {
            return true;
        }
        if (article.getCategory() == null) {
            return false;
        }
        String wanted = category.trim();
        if (article.getCategory().getId() != null && wanted.equals(String.valueOf(article.getCategory().getId()))) {
            return true;
        }
        String name = article.getCategory().getName();
        return name != null && name.toLowerCase(Locale.ROOT).contains(wanted.toLowerCase(Locale.ROOT));
    }

    static List<String> keywords(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String[] tokens = query.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        List<String> keywords = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() < 3 || STOP_WORDS.contains(token)) {
                continue;
            }
            if (!keywords.contains(token)) {
                keywords.add(token);
            }
        }
        return keywords;
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
