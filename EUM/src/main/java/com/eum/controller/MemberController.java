package com.eum.controller;

import com.eum.dto.MemberSignupRequest;
import com.eum.dto.FindPasswordRequest;
import com.eum.dto.LoginRequest;
import com.eum.dto.EmailVerificationRequest;
import com.eum.dto.EmailVerificationResponse;
import com.eum.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
// 회원가입 API 처리
public class MemberController {

    private final MemberService memberService;
    
    @PostMapping("/signup")  // http://localhost:8081/api/members/signup
    public ResponseEntity<?> signup(@RequestBody MemberSignupRequest request) {
        try {
            log.info("회원가입 API 요청 받음 - username: '{}', name: '{}', email: '{}'", 
                request.getUsername(), request.getName(), request.getEmail());
            memberService.signup(request);
            log.info("회원가입 API 응답 성공 - username: '{}'", request.getUsername());
            return ResponseEntity.ok("회원가입이 완료되었습니다.");
        } catch (RuntimeException e) {
            log.error("회원가입 API 오류 발생 - username: '{}', error: {}", 
                request.getUsername(), e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/find-password")
    public ResponseEntity<?> findPassword(@RequestBody FindPasswordRequest request) {
        String email = memberService.findPassword(request);
        return ResponseEntity.ok(Map.of(
            "message", "비밀번호가 이메일로 전송되었습니다.",
            "email", email
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            memberService.login(request);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "로그인 성공"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody EmailVerificationRequest request) {
        try {
            EmailVerificationResponse response = memberService.sendVerificationEmail(request);
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "인증번호가 이메일로 전송되었습니다.");
            if (response.getMemberId() != null) {
                responseBody.put("memberId", response.getMemberId());
            }
            if (response.getVerificationCode() != null) {
                responseBody.put("verificationCode", response.getVerificationCode());
            }
            return ResponseEntity.ok(responseBody);
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        
        log.info("인증번호 검증 요청 - email: '{}', code: '{}'", email, code);
        
        try {
            boolean isValid = memberService.verifyCode(email, code);
            
            if (isValid) {
                String memberId = memberService.findMemberIdByEmail(email);
                log.info("인증 성공 - email: '{}', memberId: '{}'", email, memberId);
                
                Map<String, String> response = new HashMap<>();
                response.put("message", "인증 성공");
                response.put("memberId", memberId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("잘못된 인증번호 - email: '{}', code: '{}'", email, code);
                Map<String, String> response = new HashMap<>();
                response.put("message", "잘못된 인증번호입니다.");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (RuntimeException e) {
            log.error("인증 처리 중 오류 발생 - email: '{}', error: {}", email, e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/find-id")
    public ResponseEntity<?> findId(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        log.info("아이디 찾기 요청 - email: '{}'", email);
        
        try {
            // 이메일 인증 상태 확인
            if (!memberService.isEmailVerified(email)) {
                log.warn("인증되지 않은 이메일로 아이디 찾기 시도 - email: '{}'", email);
                Map<String, String> response = new HashMap<>();
                response.put("message", "이메일 인증이 필요합니다.");
                return ResponseEntity.badRequest().body(response);
            }

            String memberId = memberService.findMemberIdByEmail(email);
            log.info("아이디 찾기 성공 - email: '{}', memberId: '{}'", email, memberId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "아이디 찾기 성공");
            response.put("memberId", memberId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("아이디 찾기 실패 - email: '{}', error: {}", email, e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 
