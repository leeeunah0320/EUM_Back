package com.eum.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class TtsRequest {
    private String text;
    private String voiceId = "Seoyeon";
    private String languageCode = "ko-KR";
    private String outputFormat = "mp3";
    private String engine = "neural";
    
    // 기본 생성자에서 기본값 설정
    public TtsRequest() {
        this.voiceId = "Seoyeon";
        this.languageCode = "ko-KR";
        this.outputFormat = "mp3";
        this.engine = "neural";
    }
    
    // text만 받는 생성자
    public TtsRequest(String text) {
        this.text = text;
        this.voiceId = "Seoyeon";
        this.languageCode = "ko-KR";
        this.outputFormat = "mp3";
        this.engine = "neural";
    }
}
