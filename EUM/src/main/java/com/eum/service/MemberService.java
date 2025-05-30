package com.eum.service;

import com.eum.domain.Member;
import com.eum.dto.MemberSignupRequest;
import com.eum.dto.FindPasswordRequest;
import com.eum.dto.LoginRequest;
import com.eum.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// 회원가입 로직 처리
public class MemberService {

    private final MemberRepository memberRepository;
    private final JavaMailSender emailSender;
    
    public void signup(MemberSignupRequest request) {
        // 아이디 중복 체크
        if (memberRepository.findByUsername(request.getMemberId()).isPresent()) {
            throw new RuntimeException("이미 존재하는 아이디입니다.");
        }

        Member member = Member.builder()
                .username(request.getMemberId())
                .password(request.getPassword())
                .name(request.getName())
                .email(request.getEmail())
                .build();
                
        memberRepository.save(member);
    }

    public String findPassword(FindPasswordRequest request) {
        Member member = memberRepository.findByUsernameAndNameAndEmail(
            request.getMemberId(), 
            request.getName(), 
            request.getEmail()
        ).orElseThrow(() -> new RuntimeException("일치하는 사용자가 없습니다."));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(member.getEmail());
        message.setSubject("EUM 비밀번호 찾기");
        message.setText("회원님의 비밀번호는 " + member.getPassword() + " 입니다.");
        emailSender.send(message);

        return member.getEmail();
    }

    public void login(LoginRequest request) {
        Member member = memberRepository.findByUsername(request.getMemberId())
            .orElseThrow(() -> new RuntimeException("아이디가 존재하지 않습니다."));
            
        if (!member.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }
    }
} 
