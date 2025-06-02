package com.eum.service;

import com.eum.domain.Member;
import com.eum.dto.MemberSignupRequest;
import com.eum.dto.FindPasswordRequest;
import com.eum.dto.LoginRequest;
import com.eum.dto.EmailVerificationRequest;
import com.eum.dto.EmailVerificationResponse;
import com.eum.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.Random;

@Service
@RequiredArgsConstructor
// 회원가입 로직 처리
public class MemberService {

    private final MemberRepository memberRepository;
    private final JavaMailSender emailSender;
    private final RedisService redisService;
    
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

    public EmailVerificationResponse sendVerificationEmail(EmailVerificationRequest request) {
        Member member = memberRepository.findByNameAndEmail(request.getName(), request.getEmail())
            .orElseThrow(() -> new RuntimeException("일치하는 사용자가 없습니다."));

        String verificationCode = String.format("%06d", new Random().nextInt(1000000));
        
        // Redis에 인증번호 저장 (5분 유효)
        String redisKey = "verification:" + member.getEmail();
        redisService.setDataWithExpiration(redisKey, verificationCode, 5);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(member.getEmail());
        message.setSubject("EUM 아이디 찾기 인증번호");
        message.setText("인증번호는 " + verificationCode + " 입니다.");
        emailSender.send(message);

        return EmailVerificationResponse.builder()
            .memberId(member.getUsername())
            .verificationCode(verificationCode)
            .build();
    }

    // 인증번호 검증 메서드 추가
    public boolean verifyCode(String email, String code) {
        String redisKey = "verification:" + email;
        String storedCode = redisService.getData(redisKey);
        
        if (storedCode != null && storedCode.equals(code)) {
            redisService.deleteData(redisKey);
            // 인증 성공 상태 저장
            redisService.setVerifiedStatus(email);
            return true;
        }
        return false;
    }
} 
