package com.sliit.helpdesk.knowledgebase.repository;

// Kb Category Repository is part of the campus help desk repository code.

import com.sliit.helpdesk.knowledgebase.model.KbCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KbCategoryRepository extends JpaRepository<KbCategory, Long> {

    List<KbCategory> findAllByOrderByNameAsc();

    Optional<KbCategory> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
