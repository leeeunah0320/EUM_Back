package com.eum.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ChatbotResponse {
    private String message;
    private String processedQuery;
    private String intent;
    private String confidence;
    private String sessionId;
    private boolean success;
    private String errorMessage;
    private ExtractedInfo extractedInfo;
    private String audioUrl; // AWS Polly로 생성된 음성 파일 URL
    private String audioData; // AWS Polly로 생성된 음성 데이터 (Base64 인코딩)
    private Object placeDetails; // Google Places API 결과
}
