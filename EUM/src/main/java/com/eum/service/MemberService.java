package com.eum.service;

import com.eum.domain.Member;
import com.eum.dto.MemberSignupRequest;
import com.eum.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// 회원가입 로직 처리
public class MemberService {

    private final MemberRepository memberRepository;
    
    public void signup(MemberSignupRequest request) {
        Member member = Member.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .name(request.getName())
                .email(request.getEmail())
                .build();
                
        memberRepository.save(member);
    }
} 