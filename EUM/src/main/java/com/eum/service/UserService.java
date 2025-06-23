package com.eum.service;

import com.eum.domain.User;
import com.eum.domain.Role;
import com.eum.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    public User findOrCreateUser(String email, String name, String picture, String googleId) {
        // 기존 사용자 찾기
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // 정보 업데이트
            user.setName(name);
            user.setPicture(picture);
            user.setGoogleId(googleId);
            return userRepository.save(user);
        } else {
            // 새 사용자 생성
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .picture(picture)
                    .googleId(googleId)
                    .role(Role.USER)
                    .build();
            return userRepository.save(newUser);
        }
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public Optional<User> findByGoogleId(String googleId) {
        return userRepository.findByGoogleId(googleId);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
} 
