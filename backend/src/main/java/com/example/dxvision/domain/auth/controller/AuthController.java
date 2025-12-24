package com.example.dxvision.domain.auth.controller;

import com.example.dxvision.domain.auth.dto.AuthResponse;
import com.example.dxvision.domain.auth.dto.LoginRequest;
import com.example.dxvision.domain.auth.dto.SignupRequest;
import com.example.dxvision.domain.auth.dto.UserInfoResponse;
import com.example.dxvision.domain.auth.security.CustomUserDetails;
import com.example.dxvision.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Frontend ProtectedRoute에서 호출하는 인증 확인 API
     * - Authorization: Bearer <token> 필요
     * - 성공 시 현재 로그인 사용자 정보 반환
     */
    @GetMapping("/me")
    public UserInfoResponse me(@AuthenticationPrincipal Object principal) {
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        var user = userDetails.getUser();
        return new UserInfoResponse(user.getId(), user.getEmail(), user.getName(), user.getRole());
    }
}
