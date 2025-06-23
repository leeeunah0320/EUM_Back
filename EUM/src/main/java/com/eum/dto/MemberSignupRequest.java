package com.eum.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 회원가입요청 데이터 객체체
public class MemberSignupRequest {
    private String username;
    private String password;
    private String name;
    private String email;
} 
