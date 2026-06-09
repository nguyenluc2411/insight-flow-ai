package com.insightflow.dataingestion.repository;

import com.insightflow.dataingestion.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    // Lấy danh sách event chưa gửi, ưu tiên FIFO (vào trước ra trước)
    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc(Pageable pageable);
}