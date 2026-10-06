package com.sliit.helpdesk.knowledgebase.controller;

// Article Controller is part of the campus help desk controller code.

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.service.AuthService;
import com.sliit.helpdesk.knowledgebase.dto.ArticleRequest;
import com.sliit.helpdesk.knowledgebase.dto.KbArticleResponse;
import com.sliit.helpdesk.knowledgebase.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/kb")
public class ArticleController {

    private final ArticleService articleService;
    private final AuthService authService;

    public ArticleController(ArticleService articleService, AuthService authService) {
        this.articleService = articleService;
        this.authService = authService;
    }

    @GetMapping("/articles")
    @Transactional(readOnly = true)
    public List<KbArticleResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category
    ) {
        return articleService.search(search, category).stream().map(KbArticleResponse::from).toList();
    }

    @GetMapping("/articles/{id}")
    @Transactional(readOnly = true)
    public KbArticleResponse detail(@PathVariable Long id) {
        return KbArticleResponse.from(articleService.require(id));
    }

    @GetMapping("/articles/{id}/view")
    @Transactional
    public KbArticleResponse view(@PathVariable Long id) {
        return KbArticleResponse.from(articleService.view(id));
    }

    @GetMapping("/suggest")
    @Transactional(readOnly = true)
    public List<KbArticleResponse> suggest(@RequestParam(required = false) String query) {
        return articleService.suggest(query).stream().map(KbArticleResponse::from).toList();
    }

    @PostMapping("/articles")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Transactional
    public ResponseEntity<KbArticleResponse> create(
            Authentication authentication,
            @Valid @RequestBody ArticleRequest request
    ) {
        User author = authService.requireByEmail(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(KbArticleResponse.from(articleService.create(author, request)));
    }

    @PutMapping("/articles/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Transactional
    public KbArticleResponse update(@PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
        return KbArticleResponse.from(articleService.update(id, request));
    }

    @DeleteMapping("/articles/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
