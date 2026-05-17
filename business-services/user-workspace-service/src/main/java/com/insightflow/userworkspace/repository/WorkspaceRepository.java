package com.insightflow.userworkspace.repository;


import com.insightflow.userworkspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, String> {
}