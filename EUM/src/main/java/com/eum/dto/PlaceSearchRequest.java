package com.eum.dto;

import lombok.Data;

@Data
public class PlacesSearchRequest {
    private String query;
    private String location;
    private Double latitude;
    private Double longitude;
    private Integer radius;
    private String type;
    private String language;
    private Integer maxResults;
}
