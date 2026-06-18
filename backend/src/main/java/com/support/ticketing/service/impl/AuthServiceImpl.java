package com.support.ticketing.service.impl;

import com.support.ticketing.dto.auth.AuthResponse;
import com.support.ticketing.dto.auth.LoginRequest;
import com.support.ticketing.dto.auth.RegisterRequest;
import com.support.ticketing.entity.Role;
import com.support.ticketing.entity.User;
import com.support.ticketing.exception.BadRequestException;
import com.support.ticketing.repository.UserRepository;
import com.support.ticketing.security.JwtService;
import com.support.ticketing.security.SecurityUser;
import com.support.ticketing.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email is already registered.");
        }

        User user = userRepository.save(User.builder()
                .name(request.name().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        return toAuthResponse(user, jwtService.generateToken(new SecurityUser(user)));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );
        } catch (DisabledException ex) {
            throw new BadRequestException("Your account is deactivated. Contact admin.");
        } catch (AuthenticationException ex) {
            throw new BadRequestException("Invalid email or password.");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid email or password."));
        if (!user.isActive()) {
            throw new BadRequestException("Your account is deactivated. Contact admin.");
        }

        return toAuthResponse(user, jwtService.generateToken(new SecurityUser(user)));
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }
}
