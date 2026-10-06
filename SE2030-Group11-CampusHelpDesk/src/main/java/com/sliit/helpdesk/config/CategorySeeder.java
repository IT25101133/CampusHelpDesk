package com.sliit.helpdesk.config;

// Category Seeder is part of the campus help desk config code.

import com.sliit.helpdesk.category.model.Category;
import com.sliit.helpdesk.category.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class CategorySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CategorySeeder.class);

    private static final String[][] DEFAULTS = {
            {"IT Support", "IT Services", "24"},
            {"Academic", "Faculty of Computing", "72"},
            {"Library Services", "Library", "48"},
            {"Finance Office", "Finance", "48"},
            {"Hostel", "Hostel", "48"},
            {"Facilities", "Facilities", "48"},
            {"Examinations", "Examinations", "72"},
            {"General", "General", "48"}
    };

    private final CategoryRepository categoryRepository;

    public CategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        renameLegacyFinance();
        int created = 0;
        int updated = 0;
        for (String[] row : DEFAULTS) {
            Category category = categoryRepository.findByNameIgnoreCase(row[0]).orElse(null);
            if (category == null) {
                category = new Category();
                category.setName(row[0]);
                category.setDepartment(row[1]);
                category.setSlaHours(Integer.parseInt(row[2]));
                categoryRepository.save(category);
                created++;
                continue;
            }
            boolean dirty = false;
            if (category.getDepartment() == null || !row[1].equalsIgnoreCase(category.getDepartment())) {
                category.setDepartment(row[1]);
                dirty = true;
            }
            int sla = Integer.parseInt(row[2]);
            if (category.getSlaHours() != sla) {
                category.setSlaHours(sla);
                dirty = true;
            }
            if (dirty) {
                categoryRepository.save(category);
                updated++;
            }
        }
        if (created > 0 || updated > 0) {
            log.info("Seeded {} ticket categories and synced {} department mappings", created, updated);
        }
    }

    private void renameLegacyFinance() {
        if (categoryRepository.findByNameIgnoreCase("Finance Office").isPresent()) {
            return;
        }
        categoryRepository.findByNameIgnoreCase("Finance").ifPresent(existing -> {
            existing.setName("Finance Office");
            categoryRepository.save(existing);
            log.info("Renamed ticket category Finance to Finance Office");
        });
    }
}
