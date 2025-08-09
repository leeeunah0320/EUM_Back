package com.eum.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatbotResponseData {
    private String message;
    private TtsResponse ttsResponse;
    private PlacesSearchResponse placesResponse;
}
