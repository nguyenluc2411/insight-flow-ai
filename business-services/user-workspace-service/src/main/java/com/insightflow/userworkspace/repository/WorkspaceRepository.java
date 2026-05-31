package com.insightflow.userworkspace.repository;


import com.insightflow.userworkspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, String> {
    List<Workspace> findByUserIdOrderByCreatedAtDesc(String userId);
}