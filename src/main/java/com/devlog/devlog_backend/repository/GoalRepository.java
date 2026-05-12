// GoalRepository.java
package com.devlog.devlog_backend.repository;

import com.devlog.devlog_backend.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserIdAndWeekStart(Long userId, LocalDate weekStart);
}