package com.insightflow.auth.mapper;

import com.insightflow.auth.dto.response.UserResponse;
import com.insightflow.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // roles is populated manually in UserService (loaded from user_roles table)
    @Mapping(target = "roles", ignore = true)
    UserResponse toResponse(User user);

    default UserResponse toResponse(User user, List<String> roles) {
        UserResponse response = toResponse(user);
        response.setRoles(roles);
        return response;
    }
}
