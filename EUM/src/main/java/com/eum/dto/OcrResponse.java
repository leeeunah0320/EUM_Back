package com.eum.dto;
import lombok.Data;
import java.util.List;

@Data
public class OcrResponse {
    private List<DetectedLabel> detectedLabels;
    private String translatedText;
    private String audioBase64; // Polly 음성(Base64)
    private boolean success;
    private String errorMessage;

    @Data
    public static class DetectedLabel {
        private String name;
        private float confidence;
        private String geometry;
    }
} 
