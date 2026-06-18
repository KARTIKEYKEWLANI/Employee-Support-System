package com.support.ticketing.service.impl;

import com.support.ticketing.dto.user.UserSummaryResponse;
import com.support.ticketing.dto.user.UserDetailResponse;
import com.support.ticketing.dto.user.CreateUserRequest;
import com.support.ticketing.dto.user.UpdateUserRoleRequest;
import com.support.ticketing.entity.Role;
import com.support.ticketing.entity.User;
import com.support.ticketing.exception.BadRequestException;
import com.support.ticketing.exception.ResourceNotFoundException;
import com.support.ticketing.repository.UserRepository;
import com.support.ticketing.repository.TicketRepository;
import com.support.ticketing.repository.QueueMemberRepository;
import com.support.ticketing.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TicketRepository ticketRepository;
    private final QueueMemberRepository queueMemberRepository;

    @Override
    public List<UserSummaryResponse> getAdminUsers() {
        return userRepository.findAllByRole(Role.ROLE_ADMIN).stream()
                .map(user -> new UserSummaryResponse(user.getId(), user.getName(), user.getEmail(), user.isActive()))
                .toList();
    }

    @Override
    public List<UserSummaryResponse> getAssignableUsers() {
        return userRepository.findAllByRoleIn(List.of(Role.ROLE_ADMIN, Role.ROLE_AGENT)).stream()
                .filter(User::isActive)
                .map(user -> new UserSummaryResponse(user.getId(), user.getName(), user.getEmail(), user.isActive()))
                .toList();
    }

    @Override
    public List<UserDetailResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDetailResponse)
                .toList();
    }

    @Override
    public UserDetailResponse createUser(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already exists.");
        }
        Role role;
        try {
            role = Role.valueOf(request.role().trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role.");
        }
        User user = userRepository.save(User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .active(true)
                .build());
        return toDetailResponse(user);
    }

    @Override
    public UserDetailResponse updateRole(Long userId, UpdateUserRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if ("admin@support.com".equalsIgnoreCase(user.getEmail()) && !"ROLE_ADMIN".equals(request.role().trim())) {
            throw new BadRequestException("System admin role cannot be changed.");
        }
        Role role;
        try {
            role = Role.valueOf(request.role().trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role.");
        }
        user.setRole(role);
        return toDetailResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDetailResponse deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if ("admin@support.com".equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("System admin cannot be deactivated.");
        }
        queueMemberRepository.deleteAllByUser(user);
        List<com.support.ticketing.entity.Ticket> assigned = ticketRepository.findAllByAssignedTo(user);
        assigned.forEach(ticket -> ticket.setAssignedTo(null));
        List<com.support.ticketing.entity.Ticket> team = ticketRepository.findAllByAssigneesContaining(user);
        team.forEach(ticket -> ticket.getAssignees().remove(user));
        ticketRepository.saveAll(assigned);
        ticketRepository.saveAll(team);
        user.setActive(false);
        return toDetailResponse(userRepository.save(user));
    }

    @Override
    public UserDetailResponse activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        user.setActive(true);
        return toDetailResponse(userRepository.save(user));
    }

    private UserDetailResponse toDetailResponse(User user) {
        return new UserDetailResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), user.isActive());
    }
}
