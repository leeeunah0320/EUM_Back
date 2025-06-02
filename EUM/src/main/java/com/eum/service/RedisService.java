package com.eum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, String> redisTemplate;

    public void setDataWithExpiration(String key, String value, long minutes) {
        redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(minutes));
    }

    public String getData(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteData(String key) {
        redisTemplate.delete(key);
    }

    // 인증 상태 저장 (5분 유효)
    public void setVerifiedStatus(String email) {
        String verifiedKey = "verified:" + email;
        redisTemplate.opsForValue().set(verifiedKey, "true", Duration.ofMinutes(5));
    }

    // 인증 상태 확인
    public boolean isVerified(String email) {
        String verifiedKey = "verified:" + email;
        String status = redisTemplate.opsForValue().get(verifiedKey);
        return "true".equals(status);
    }
} 
