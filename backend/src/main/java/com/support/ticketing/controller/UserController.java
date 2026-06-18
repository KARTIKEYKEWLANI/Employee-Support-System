package com.support.ticketing.controller;

import com.support.ticketing.dto.user.UserSummaryResponse;
import com.support.ticketing.dto.user.UserDetailResponse;
import com.support.ticketing.dto.user.CreateUserRequest;
import com.support.ticketing.dto.user.UpdateUserRoleRequest;
import com.support.ticketing.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.support.ticketing.security.SecurityUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/admins")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserSummaryResponse> getAdmins() {
        return userService.getAdminUsers();
    }

    @GetMapping("/assignable")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserSummaryResponse> getAssignable() {
        return userService.getAssignableUsers();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDetailResponse> getUsers() {
        return userService.getAllUsers();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserDetailResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDetailResponse updateRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        return userService.updateRole(id, request);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDetailResponse deactivate(@PathVariable Long id) {
        return userService.deactivateUser(id);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDetailResponse activate(@PathVariable Long id) {
        return userService.activateUser(id);
    }

    @DeleteMapping("/me")
    public UserDetailResponse deactivateSelf(@AuthenticationPrincipal SecurityUser currentUser) {
        return userService.deactivateUser(currentUser.getId());
    }
}
