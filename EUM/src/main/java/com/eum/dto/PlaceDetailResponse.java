package com.eum.dto;

import lombok.Data;
import java.util.List;

@Data
public class PlaceDetailResponse {
    private String status;
    private PlaceDetail result;
    
    @Data
    public static class PlaceDetail {
        private String placeId;
        private String name;
        private String formattedAddress;
        private String formattedPhoneNumber;
        private String internationalPhoneNumber;
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
        private String website;
        private String url;
        private List<Review> reviews;
        private List<String> weekdayText;
        private String utcOffset;
        private String adrAddress;
        private String plusCode;
        private String reference;
        private String scope;
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
    
    @Data
    public static class Review {
        private String authorName;
        private String authorUrl;
        private String language;
        private String profilePhotoUrl;
        private Integer rating;
        private String relativeTimeDescription;
        private String text;
        private Long time;
        private Boolean translated;
    }
}
