package com.eum.service;

import com.eum.dto.GoogleAuthResponse;
import com.eum.domain.User;
import com.eum.repository.UserRepository;
import com.eum.service.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.google-android.client-id}")
    private String googleClientId;

    public GoogleAuthResponse authenticateWithGoogle(String idTokenString) throws Exception {
        if (idTokenString == null || idTokenString.isEmpty()) {
            throw new Exception("ID 토큰이 제공되지 않았습니다.");
        }

        try {
            // Google ID 토큰 검증
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new Exception("유효하지 않은 ID 토큰입니다.");
            }

            Payload payload = idToken.getPayload();

            // 사용자 정보 추출
            String userId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");
            boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());

            if (!emailVerified) {
                throw new Exception("이메일이 인증되지 않았습니다.");
            }

            // 사용자 정보 저장 또는 업데이트
            User user = userRepository.findByEmail(email)
                    .orElse(new User());

            user.setEmail(email);
            user.setName(name);
            user.setPicture(picture);
            user.setGoogleId(userId);
            user.setProvider("google");

            userRepository.save(user);

            // JWT 토큰 생성
            String jwtToken = jwtService.generateToken(user.getEmail(), user.getName(), user.getRole().name());

            return GoogleAuthResponse.builder()
                    .success(true)
                    .message("구글 로그인 성공")
                    .accessToken(jwtToken)
                    .email(email)
                    .name(name)
                    .picture(picture)
                    .build();

        } catch (Exception e) {
            throw new Exception("구글 인증 실패: " + e.getMessage());
        }
    }

    

    private String extractValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
} 
