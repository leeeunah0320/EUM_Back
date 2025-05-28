package com.eum.controller;

import com.eum.dto.MemberSignupRequest;
import com.eum.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
// 회원가입 API 처리
public class MemberController {

    private final MemberService memberService;
    
    @PostMapping("/signup")  // http://localhost:8081/api/members/signup
    public ResponseEntity<String> signup(@RequestBody MemberSignupRequest request) {
        memberService.signup(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }
} 