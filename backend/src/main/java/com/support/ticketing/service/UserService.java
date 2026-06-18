package com.support.ticketing.service;

import com.support.ticketing.dto.user.UserSummaryResponse;
import com.support.ticketing.dto.user.UserDetailResponse;
import com.support.ticketing.dto.user.CreateUserRequest;
import com.support.ticketing.dto.user.UpdateUserRoleRequest;
import java.util.List;

public interface UserService {
    List<UserSummaryResponse> getAdminUsers();

    List<UserSummaryResponse> getAssignableUsers();

    List<UserDetailResponse> getAllUsers();

    UserDetailResponse createUser(CreateUserRequest request);

    UserDetailResponse updateRole(Long userId, UpdateUserRoleRequest request);

    UserDetailResponse deactivateUser(Long userId);

    UserDetailResponse activateUser(Long userId);
}
