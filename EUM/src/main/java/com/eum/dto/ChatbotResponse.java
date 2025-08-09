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
    private TtsResponse ttsResponse;
    private PlacesSearchResponse placesResponse;
} 
