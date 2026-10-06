package com.sliit.helpdesk.knowledgebase;

// Article Service Test is part of the campus help desk knowledgebase code.

import com.sliit.helpdesk.knowledgebase.model.KbArticle;
import com.sliit.helpdesk.knowledgebase.model.KbCategory;
import com.sliit.helpdesk.knowledgebase.repository.ArticleRepository;
import com.sliit.helpdesk.knowledgebase.repository.KbCategoryRepository;
import com.sliit.helpdesk.knowledgebase.service.ArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock private ArticleRepository articleRepository;
    @Mock private KbCategoryRepository categoryRepository;
    private ArticleService articleService;

    @BeforeEach
    void setUp() {
        articleService = new ArticleService(articleRepository, categoryRepository);
    }

    @Test
    void searchFiltersByKeywordInTitleOrContent() {
        KbArticle password = article(1L, "Reset your campus password", "Use account.sliit.lk", "Accounts & access");
        KbArticle classroom = article(2L, "Report a classroom fault", "Call campus Facilities for AV issues", "Campus facilities");
        when(articleRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(password, classroom));

        assertThat(articleService.search("password", null)).containsExactly(password);
        assertThat(articleService.search("facilities", null)).containsExactly(classroom);
        assertThat(articleService.search("campus", null)).containsExactly(password, classroom);
    }

    @Test
    void searchFiltersByCategoryNameOrId() {
        KbArticle password = article(1L, "Reset your campus password", "Use the portal", "Accounts & access");
        password.getCategory().setId(4L);
        KbArticle classroom = article(2L, "Report a classroom fault", "Call Facilities", "Campus facilities");
        classroom.getCategory().setId(5L);
        when(articleRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(password, classroom));

        assertThat(articleService.search(null, "Accounts")).containsExactly(password);
        assertThat(articleService.search(null, "5")).containsExactly(classroom);
        assertThat(articleService.search("reset", "Campus facilities")).isEmpty();
        assertThat(articleService.search("reset", "Accounts & access")).containsExactly(password);
    }

    @Test
    void viewIncrementsCountForArticle() {
        KbArticle article = article(3L, "Reset your campus password", "Use the portal", "Accounts");
        article.setViewCount(10);
        when(articleRepository.findById(3L)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(KbArticle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KbArticle viewed = articleService.view(3L);

        assertThat(viewed.getViewCount()).isEqualTo(11);
    }

    @Test
    void viewRejectsUnknownArticle() {
        when(articleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.view(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Article not found");
    }

    @Test
    void suggestReturnsArticlesMatchingKeywords() {
        KbArticle password = article(1L, "Reset your campus password", "Use account.sliit.lk", "Accounts");
        when(articleRepository.search(anyString())).thenAnswer(invocation -> {
            String keyword = invocation.getArgument(0);
            return "password".equals(keyword) || "reset".equals(keyword) ? List.of(password) : List.of();
        });

        assertThat(articleService.suggest("Cannot reset my campus password")).containsExactly(password);
        assertThat(articleService.suggest("a to the")).isEmpty();
    }

    private static KbArticle article(Long id, String title, String content, String categoryName) {
        KbCategory category = new KbCategory();
        category.setName(categoryName);
        KbArticle article = new KbArticle();
        article.setId(id);
        article.setTitle(title);
        article.setContent(content);
        article.setCategory(category);
        return article;
    }
}
