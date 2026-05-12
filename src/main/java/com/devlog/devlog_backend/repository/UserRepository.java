// UserRepository.java
package com.devlog.devlog_backend.repository;

import com.devlog.devlog_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// WHY JpaRepository<User, Long>: User is the entity, Long is the ID type.
// This gives us save(), findById(), findAll(), delete() for free.
public interface UserRepository extends JpaRepository<User, Long> {
    // WHY Optional<User>: the user might not exist. Optional forces us
    // to handle the "not found" case instead of getting a NullPointerException.
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}