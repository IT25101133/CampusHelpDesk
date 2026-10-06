package com.sliit.helpdesk.knowledgebase.dto;

// Kb Article Response is part of the campus help desk dto code.

import com.sliit.helpdesk.knowledgebase.model.KbArticle;

import java.time.LocalDateTime;

public class KbArticleResponse {

    private Long id;
    private String title;
    private String content;
    private Long categoryId;
    private String category;
    private String author;
    private int viewCount;
    private LocalDateTime createdAt;

    public static KbArticleResponse from(KbArticle article) {
        KbArticleResponse response = new KbArticleResponse();
        response.id = article.getId();
        response.title = article.getTitle();
        response.content = article.getContent();
        response.categoryId = article.getCategory() == null ? null : article.getCategory().getId();
        response.category = article.getCategory() == null ? null : article.getCategory().getName();
        response.author = article.getAuthor() == null ? null : article.getAuthor().getFullName();
        response.viewCount = article.getViewCount();
        response.createdAt = article.getCreatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategory() {
        return category;
    }

    public String getAuthor() {
        return author;
    }

    public int getViewCount() {
        return viewCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
