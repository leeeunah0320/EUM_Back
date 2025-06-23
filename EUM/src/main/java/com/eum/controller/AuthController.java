package com.eum.controller;

import com.eum.dto.GoogleAuthRequest;
import com.eum.dto.GoogleAuthResponse;
import com.eum.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    public ResponseEntity<GoogleAuthResponse> googleAuth(@RequestBody GoogleAuthRequest request) {
        try {
            GoogleAuthResponse response = authService.authenticateWithGoogle(request.getIdToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(GoogleAuthResponse.builder()
                    .success(false)
                    .message("인증 실패: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth API is working!");
    }
} 
