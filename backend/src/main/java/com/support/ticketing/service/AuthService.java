package com.support.ticketing.service;

import com.support.ticketing.dto.auth.AuthResponse;
import com.support.ticketing.dto.auth.LoginRequest;
import com.support.ticketing.dto.auth.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
