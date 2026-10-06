package com.sliit.helpdesk.knowledgebase.repository;

import com.sliit.helpdesk.knowledgebase.model.KbCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KbCategoryRepository extends JpaRepository<KbCategory, Long> {

    List<KbCategory> findAllByOrderByNameAsc();
}
