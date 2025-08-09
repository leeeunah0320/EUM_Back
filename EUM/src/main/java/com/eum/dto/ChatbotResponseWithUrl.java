package com.eum.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ChatbotResponseWithUrl {
    private String text;           // 응답 텍스트
    private String audioUrl;       // Data URL 형태의 음성 파일 (S3 대신)
    private boolean success;       // 성공 여부
    private String errorMessage;   // 오류 메시지 (실패 시)
    private String sessionId;      // 세션 ID
    private String intent;         // 의도 분석 결과
    private String confidence;     // 신뢰도
}
