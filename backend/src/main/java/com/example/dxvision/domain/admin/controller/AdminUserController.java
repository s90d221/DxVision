package com.example.dxvision.domain.admin.controller;

import com.example.dxvision.domain.admin.dto.AdminUserDetailResponse;
import com.example.dxvision.domain.admin.dto.AdminUserListItem;
import com.example.dxvision.domain.admin.dto.AdminUserUpdateRequest;
import com.example.dxvision.domain.admin.dto.PageResponse;
import com.example.dxvision.domain.admin.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public PageResponse<AdminUserListItem> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "q", required = false) String q
    ) {
        return adminUserService.listUsers(page, size, q);
    }

    @GetMapping("/{userId}")
    public AdminUserDetailResponse getUser(@PathVariable Long userId) {
        return adminUserService.getUserDetail(userId);
    }

    @PatchMapping("/{userId}")
    public AdminUserDetailResponse updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        return adminUserService.updateUserStatus(userId, request);
    }
}
