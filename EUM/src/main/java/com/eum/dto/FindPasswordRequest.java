package com.eum.dto;

import lombok.Getter;

@Getter
public class FindPasswordRequest {
    private String memberId;
    private String name;
    private String email;
} 
