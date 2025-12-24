package com.example.dxvision.domain.attempt.controller;

import com.example.dxvision.domain.attempt.dto.AttemptResultResponse;
import com.example.dxvision.domain.attempt.dto.AttemptSubmitRequest;
import com.example.dxvision.domain.attempt.service.AttemptService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attempts")
public class AttemptController {
    private final AttemptService attemptService;

    public AttemptController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping
    public AttemptResultResponse submit(@Valid @RequestBody AttemptSubmitRequest request) {
        return attemptService.submitAttempt(request);
    }
}
