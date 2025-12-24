package com.example.dxvision.domain.auth.service;

import com.example.dxvision.domain.auth.Role;
import com.example.dxvision.domain.auth.User;
import com.example.dxvision.domain.auth.dto.AuthResponse;
import com.example.dxvision.domain.auth.dto.LoginRequest;
import com.example.dxvision.domain.auth.dto.SignupRequest;
import com.example.dxvision.domain.auth.dto.UserInfoResponse;
import com.example.dxvision.domain.auth.security.CustomUserDetails;
import com.example.dxvision.domain.auth.security.JwtTokenProvider;
import com.example.dxvision.domain.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse signup(SignupRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.email(), encodedPassword, request.name(), Role.USER);
        User savedUser = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String token = jwtTokenProvider.createToken(userDetails);
        return new AuthResponse(token, toUserInfo(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtTokenProvider.createToken(userDetails);
        return new AuthResponse(token, toUserInfo(userDetails.getUser()));
    }

    private UserInfoResponse toUserInfo(User user) {
        return new UserInfoResponse(user.getId(), user.getEmail(), user.getName(), user.getRole());
    }
}
