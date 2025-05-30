package com.eum.controller;

import com.eum.dto.MemberSignupRequest;
import com.eum.dto.FindPasswordRequest;
import com.eum.dto.LoginRequest;
import com.eum.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
// 회원가입 API 처리
public class MemberController {

    private final MemberService memberService;
    
    @PostMapping("/signup")  // http://localhost:8081/api/members/signup
    public ResponseEntity<?> signup(@RequestBody MemberSignupRequest request) {
        try {
            memberService.signup(request);
            return ResponseEntity.ok("회원가입이 완료되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", e.getMessage()
            ));
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
} 
