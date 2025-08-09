package com.eum.dto;

import lombok.Data;
import java.util.List;

@Data
public class PlacesSearchResponse {
    private String status;
    private List<PlaceResult> results;
    private String nextPageToken;
    
    @Data
    public static class PlaceResult {
        private String placeId;
        private String name;
        private String formattedAddress;
        private Geometry geometry;
        private List<String> types;
        private Double rating;
        private Integer userRatingsTotal;
        private OpeningHours openingHours;
        private String businessStatus;
        private String icon;
        private String iconBackgroundColor;
        private String iconMaskBaseUri;
        private List<Photos> photos;
        private String priceLevel;
        private String vicinity;
    }
    
    @Data
    public static class Geometry {
        private Location location;
        private Viewport viewport;
    }
    
    @Data
    public static class Location {
        private Double lat;
        private Double lng;
    }
    
    @Data
    public static class Viewport {
        private Location northeast;
        private Location southwest;
    }
    
    @Data
    public static class OpeningHours {
        private Boolean openNow;
        private List<Period> periods;
        private List<String> weekdayText;
    }
    
    @Data
    public static class Period {
        private DayTime open;
        private DayTime close;
    }
    
    @Data
    public static class DayTime {
        private Integer day;
        private String time;
    }
    
    @Data
    public static class Photos {
        private Integer height;
        private List<String> htmlAttributions;
        private String photoReference;
        private Integer width;
    }
}
