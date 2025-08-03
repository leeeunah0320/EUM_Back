package com.eum.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class GeminiRequest {
    private List<Content> contents;
    private GenerationConfig generationConfig;
    
    @Data
    @Builder
    public static class Content {
        private List<Part> parts;
    }
    
    @Data
    @Builder
    public static class Part {
        private String text;
    }
    
    @Data
    @Builder
    public static class GenerationConfig {
        private int maxOutputTokens;
        private double temperature;
        private double topP;
        private double topK;
    }
} 
