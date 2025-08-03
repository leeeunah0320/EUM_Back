package com.eum.dto;

import lombok.Data;

@Data
public class ChatbotRequest {
    private String message;
    private String audioData; // Base64 인코딩된 오디오 데이터
    private String sessionId;
    private String userId;
} 
