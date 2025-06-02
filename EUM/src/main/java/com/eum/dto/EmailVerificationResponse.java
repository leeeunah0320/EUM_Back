package com.eum.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailVerificationResponse {
    private String memberId;
    private String verificationCode;
} 
