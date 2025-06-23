package com.eum.service;

import com.eum.dto.GoogleAuthResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public GoogleAuthResponse authenticateWithGoogle(String idTokenString) throws Exception {
        // 간단한 토큰 검증 (실제로는 JWT 토큰 검증 로직 구현 필요)
        if (idTokenString != null && !idTokenString.isEmpty()) {
            // 여기서 실제 ID 토큰 검증 로직을 구현할 수 있습니다
            // 현재는 간단한 응답만 반환
            
            return GoogleAuthResponse.builder()
                    .success(true)
                    .message("인증 성공")
                    .accessToken("generated-access-token")
                    .email("user@example.com")
                    .name("사용자")
                    .picture("https://example.com/picture.jpg")
                    .build();
        } else {
            throw new Exception("Invalid ID token");
        }
    }
} 
