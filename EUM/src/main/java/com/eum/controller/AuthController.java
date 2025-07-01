package com.eum.controller;

import com.eum.dto.GoogleAuthRequest;
import com.eum.dto.GoogleAuthResponse;
import com.eum.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    public ResponseEntity<GoogleAuthResponse> googleAuth(@RequestBody GoogleAuthRequest request) {
        log.info("=== 구글 로그인 요청 (웹) ===");
        try {
            GoogleAuthResponse response = authService.authenticateWithGoogle(request.getIdToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("구글 로그인 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(GoogleAuthResponse.builder()
                    .success(false)
                    .message("인증 실패: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/google/mobile")
    public ResponseEntity<GoogleAuthResponse> googleAuthMobile(@RequestBody GoogleAuthRequest request) {
        log.info("=== 구글 로그인 요청 (모바일) ===");
        log.info("ID Token 길이: {}", request.getIdToken() != null ? request.getIdToken().length() : 0);
        try {
            GoogleAuthResponse response = authService.authenticateWithGoogle(request.getIdToken());
            log.info("구글 로그인 성공");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("구글 모바일 로그인 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(GoogleAuthResponse.builder()
                    .success(false)
                    .message("모바일 인증 실패: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.info("=== 테스트 요청 ===");
        return ResponseEntity.ok("Auth API is working!");
    }
} 
