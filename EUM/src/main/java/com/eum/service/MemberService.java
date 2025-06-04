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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Service
@RequiredArgsConstructor
// 회원가입 로직 처리
public class MemberService {

    private final MemberRepository memberRepository;
    private final JavaMailSender emailSender;
    private final RedisService redisService;
    private static final Logger log = LoggerFactory.getLogger(MemberService.class);
    
    public void signup(MemberSignupRequest request) {
        log.info("회원가입 요청 받음 - username: '{}', name: '{}', email: '{}'",
            request.getUsername(), request.getName(), request.getEmail());

        // 아이디 중복 체크
        if (memberRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("회원가입 실패 - 이미 존재하는 아이디: {}", request.getUsername());
            throw new RuntimeException("이미 존재하는 아이디입니다.");
        }

        // 이메일 중복 체크
        if (memberRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("회원가입 실패 - 이미 존재하는 이메일: {}", request.getEmail());
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }

        Member member = Member.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .name(request.getName())
                .email(request.getEmail())
                .build();
                
        Member savedMember = memberRepository.save(member);
        log.info("회원가입 완료 - id: {}, username: '{}', name: '{}', email: '{}'",
            savedMember.getId(), savedMember.getUsername(), 
            savedMember.getName(), savedMember.getEmail());
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
        if (request.getName() == null || request.getEmail() == null) {
            throw new RuntimeException("이름과 이메일은 필수 입력 항목입니다.");
        }

        log.info("이메일 인증 요청 - 이름 길이: {}, 값: '{}', 이메일 길이: {}, 값: '{}'", 
            request.getName().length(), request.getName(),
            request.getEmail().length(), request.getEmail());
        
        // 모든 회원 정보 로깅
        log.info("=== 전체 회원 목록 ===");
        memberRepository.findAll().forEach(m -> 
            log.info("저장된 회원 - ID: {}, 이름: '{}', 이메일: '{}'", 
                m.getId(), m.getName(), m.getEmail())
        );
        
        List<Member> members = memberRepository.findAllByNameAndEmail(request.getName(), request.getEmail());
        
        if (members.isEmpty()) {
            throw new RuntimeException("해당 이름과 이메일로 등록된 회원이 없습니다.");
        }
        
        if (members.size() > 1) {
            log.warn("중복된 회원 발견 - 이름: '{}', 이메일: '{}', 찾은 회원 수: {}", 
                request.getName(), request.getEmail(), members.size());
            throw new RuntimeException("중복된 회원이 발견되었습니다. 관리자에게 문의해주세요.");
        }

        Member member = members.get(0);
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

    // 인증번호 검증 메서드
    public boolean verifyCode(String email, String code) {
        log.info("인증번호 검증 시작 - email: '{}', code: '{}'", email, code);
        
        String redisKey = "verification:" + email;
        String storedCode = redisService.getData(redisKey);
        
        log.info("Redis에 저장된 인증번호 - email: '{}', storedCode: '{}'", email, storedCode);
        
        if (storedCode == null) {
            log.warn("저장된 인증번호 없음 - email: '{}'", email);
            return false;
        }
        
        if (storedCode.equals(code)) {
            log.info("인증번호 일치 - email: '{}'", email);
            redisService.deleteData(redisKey);
            redisService.setVerifiedStatus(email);
            return true;
        }
        
        log.warn("인증번호 불일치 - email: '{}', 입력된 코드: '{}', 저장된 코드: '{}'", 
            email, code, storedCode);
        return false;
    }

    // 이메일로 회원 아이디 찾기
    public String findMemberIdByEmail(String email) {
        log.info("이메일로 회원 아이디 찾기 - email: '{}'", email);
        
        List<Member> members = memberRepository.findAllByEmail(email);
        
        if (members.isEmpty()) {
            log.warn("회원을 찾을 수 없음 - email: '{}'", email);
            throw new RuntimeException("해당 이메일로 등록된 회원이 없습니다.");
        }
        
        if (members.size() > 1) {
            log.warn("중복된 이메일 발견 - email: '{}', 찾은 회원 수: {}", email, members.size());
            throw new RuntimeException("중복된 계정이 발견되었습니다. 관리자에게 문의해주세요.");
        }
        
        String memberId = members.get(0).getUsername();
        log.info("회원 아이디 찾기 성공 - email: '{}', memberId: '{}'", email, memberId);
        return memberId;
    }

    public boolean isEmailVerified(String email) {
        return redisService.isVerified(email);
    }
} 
