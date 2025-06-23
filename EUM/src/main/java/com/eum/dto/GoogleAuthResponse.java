package com.eum.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoogleAuthResponse {
    private boolean success;
    private String message;
    private String accessToken;
    private String email;
    private String name;
    private String picture;
} 
