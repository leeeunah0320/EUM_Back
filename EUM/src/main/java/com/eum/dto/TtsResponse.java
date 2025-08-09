package com.eum.dto;

import lombok.Data;

@Data
public class TtsResponse {
    private String audioUrl;
    private String audioBase64;
    private String text;
    private String voiceId;
    private String languageCode;
    private String format;
    private Long duration;
}
