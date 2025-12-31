package com.example.dxvision.domain.auth.controller;

import com.example.dxvision.domain.auth.dto.UpdateUserRequest;
import com.example.dxvision.domain.auth.dto.UserInfoResponse;
import com.example.dxvision.domain.auth.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @PatchMapping("/me")
    public UserInfoResponse updateMe(@Valid @RequestBody UpdateUserRequest request) {
        return userProfileService.updateCurrentUser(request);
    }
}
