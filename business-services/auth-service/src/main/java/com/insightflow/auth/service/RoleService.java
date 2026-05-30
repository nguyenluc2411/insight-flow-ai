package com.insightflow.auth.service;

import com.insightflow.auth.dto.response.RoleResponse;

import java.util.List;

public interface RoleService {

    List<RoleResponse> listAllRoles();
}
