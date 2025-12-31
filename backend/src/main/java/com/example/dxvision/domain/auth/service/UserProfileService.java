package com.example.dxvision.domain.auth.service;

import com.example.dxvision.domain.auth.User;
import com.example.dxvision.domain.auth.dto.UpdateUserRequest;
import com.example.dxvision.domain.auth.dto.UserInfoResponse;
import com.example.dxvision.domain.repository.UserRepository;
import com.example.dxvision.global.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {
    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;

    public UserProfileService(CurrentUserProvider currentUserProvider, UserRepository userRepository) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
    }

    @Transactional
    public UserInfoResponse updateCurrentUser(UpdateUserRequest request) {
        User user = currentUserProvider.getCurrentUser();
        user.updateName(request.name().trim());
        User saved = userRepository.save(user);
        return new UserInfoResponse(saved.getId(), saved.getEmail(), saved.getName(), saved.getRole());
    }
}
