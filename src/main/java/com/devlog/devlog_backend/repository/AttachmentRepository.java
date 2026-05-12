// AttachmentRepository.java
package com.devlog.devlog_backend.repository;

import com.devlog.devlog_backend.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByEntryId(Long entryId);
}