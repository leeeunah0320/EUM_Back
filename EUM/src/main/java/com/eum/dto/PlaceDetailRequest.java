package com.eum.dto;

import lombok.Data;

@Data
public class PlaceDetailRequest {
    private String placeId;
    private String language;
    private String fields;
}
