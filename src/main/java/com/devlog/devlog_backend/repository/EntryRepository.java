// EntryRepository.java
package com.devlog.devlog_backend.repository;

import com.devlog.devlog_backend.entity.Entry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntryRepository extends JpaRepository<Entry, Long> {

    // WHY Pageable: instead of loading ALL entries, we load page by page.
    // This is essential for performance — you never want GET /entries to
    // return 10,000 rows at once.
    Page<Entry> findByUserIdOrderByEntryDateDesc(Long userId, Pageable pageable);

    // WHY @Query with JPQL: when the method name gets too complex,
    // we write the query manually. JPQL uses entity/field names, not table/column names.
    @Query("SELECT e FROM Entry e WHERE e.user.id = :userId " +
            "AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(e.body) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Entry> searchEntries(@Param("userId") Long userId,
                              @Param("search") String search,
                              Pageable pageable);
}