package com.eum.controller;

import com.eum.domain.User;
import com.eum.service.UserService;
import com.eum.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;
    private final JwtService jwtService;

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal != null) {
            String email = null;
            String name = null;
            String picture = null;
            String socialId = null;

            // 구글 등 기본 OAuth2 사용자
            email = principal.getAttribute("email");
            name = principal.getAttribute("name");
            picture = principal.getAttribute("picture");
            socialId = principal.getName();

            // DB에 사용자 정보 저장/업데이트
            User user = userService.findOrCreateUser(email, name, picture, socialId);
            // JWT 토큰 생성
            String token = jwtService.generateToken(user.getEmail(), user.getName(), user.getRole().name());
            // 모델에 정보 추가
            model.addAttribute("userName", user.getName());
            model.addAttribute("userEmail", user.getEmail());
            model.addAttribute("userPicture", user.getPicture());
            model.addAttribute("jwtToken", token);
            model.addAttribute("userId", user.getId());
        }
        return "dashboard";
    }
    
    @GetMapping("/api/user/token")
    @ResponseBody
    public Map<String, Object> getToken(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> response = new HashMap<>();
        if (principal != null) {
            String email = null;
            String name = null;
            String picture = null;
            String socialId = null;

            // 구글 등 기본 OAuth2 사용자
            email = principal.getAttribute("email");
            name = principal.getAttribute("name");
            picture = principal.getAttribute("picture");
            socialId = principal.getName();

            User user = userService.findOrCreateUser(email, name, picture, socialId);
            String token = jwtService.generateToken(user.getEmail(), user.getName(), user.getRole().name());
            response.put("success", true);
            response.put("token", token);
            response.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "picture", user.getPicture(),
                "role", user.getRole().name()
            ));
        } else {
            response.put("success", false);
            response.put("message", "인증되지 않은 사용자");
        }
        return response;
    }
} 
