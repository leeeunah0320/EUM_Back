package com.eum.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GooglePlacesService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${google.places.api-key}")
    private String apiKey;

    @Value("${google.places.base-url:https://maps.googleapis.com/maps/api/place}")
    private String baseUrl;

    /**
     * Google Places API를 사용하여 장소 검색
     */
    public List<Map<String, Object>> searchPlaces(String query, String location, int radius) {
        try {
            log.info("Google Places 검색 시작: query={}, location={}, radius={}", query, location, radius);

            String url = String.format("%s/textsearch/json?query=%s&key=%s&language=ko", 
                baseUrl, query, apiKey);

            if (location != null && !location.trim().isEmpty()) {
                url += String.format("&location=%s&radius=%d", location, radius);
            }

            String response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return parsePlacesResponse(response);

        } catch (Exception e) {
            log.error("Google Places 검색 중 오류 발생", e);
            return new ArrayList<>();
        }
    }

    /**
     * Place ID를 사용하여 장소 상세 정보 조회
     */
    public Map<String, Object> getPlaceDetails(String placeId) {
        try {
            log.info("Google Places 상세 정보 조회: placeId={}", placeId);

            String url = String.format("%s/details/json?place_id=%s&fields=name,rating,opening_hours,reviews,formatted_address,geometry&key=%s&language=ko", 
                baseUrl, placeId, apiKey);

            String response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return parsePlaceDetailsResponse(response);

        } catch (Exception e) {
            log.error("Google Places 상세 정보 조회 중 오류 발생", e);
            return Map.of();
        }
    }

    /**
     * Places API 응답 파싱
     */
    private List<Map<String, Object>> parsePlacesResponse(String response) {
        List<Map<String, Object>> places = new ArrayList<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode resultsNode = rootNode.get("results");

            if (resultsNode != null && resultsNode.isArray()) {
                for (JsonNode placeNode : resultsNode) {
                    Map<String, Object> place = Map.of(
                        "place_id", placeNode.get("place_id").asText(),
                        "name", placeNode.get("name").asText(),
                        "rating", placeNode.has("rating") ? placeNode.get("rating").asDouble() : 0.0,
                        "formatted_address", placeNode.has("formatted_address") ? 
                            placeNode.get("formatted_address").asText() : "",
                        "geometry", placeNode.get("geometry")
                    );
                    places.add(place);
                }
            }

        } catch (Exception e) {
            log.error("Places API 응답 파싱 중 오류 발생", e);
        }

        return places;
    }

    /**
     * Place Details API 응답 파싱 (한국어 번역 포함)
     */
    private Map<String, Object> parsePlaceDetailsResponse(String response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode resultNode = rootNode.get("result");

            if (resultNode != null) {
                Map<String, Object> details = new HashMap<>();
                
                // 기본 정보
                details.put("name", resultNode.has("name") ? resultNode.get("name").asText() : "");
                details.put("rating", resultNode.has("rating") ? resultNode.get("rating").asDouble() : 0.0);
                details.put("formatted_address", resultNode.has("formatted_address") ? 
                    resultNode.get("formatted_address").asText() : "");
                
                // 영업시간 정보 (한국어로 번역)
                if (resultNode.has("opening_hours")) {
                    details.put("opening_hours", translateOpeningHours(resultNode.get("opening_hours")));
                } else {
                    details.put("opening_hours", objectMapper.createObjectNode());
                }
                
                // 리뷰 정보 (한국어로 번역)
                if (resultNode.has("reviews")) {
                    details.put("reviews", translateReviews(resultNode.get("reviews")));
                } else {
                    details.put("reviews", objectMapper.createArrayNode());
                }
                
                return details;
            }

        } catch (Exception e) {
            log.error("Place Details API 응답 파싱 중 오류 발생", e);
        }

        return Map.of();
    }
    
    /**
     * 영업시간을 한국어로 번역
     */
    private Object translateOpeningHours(JsonNode openingHours) {
        try {
            Map<String, Object> translatedHours = new HashMap<>();
            
            if (openingHours.has("weekday_text")) {
                List<String> weekdayText = new ArrayList<>();
                for (JsonNode dayText : openingHours.get("weekday_text")) {
                    String translated = translateDayText(dayText.asText());
                    weekdayText.add(translated);
                }
                translatedHours.put("weekday_text", weekdayText);
            }
            
            return translatedHours;
        } catch (Exception e) {
            log.error("영업시간 번역 중 오류 발생", e);
            return openingHours;
        }
    }
    
    /**
     * 요일 텍스트를 한국어로 번역
     */
    private String translateDayText(String dayText) {
        // 영어 요일을 한국어로 번역
        String translated = dayText
            .replace("Monday", "월요일")
            .replace("Tuesday", "화요일")
            .replace("Wednesday", "수요일")
            .replace("Thursday", "목요일")
            .replace("Friday", "금요일")
            .replace("Saturday", "토요일")
            .replace("Sunday", "일요일")
            .replace("AM", "오전")
            .replace("PM", "오후")
            .replace("–", "–");
            
        return translated;
    }
    
    /**
     * 리뷰를 한국어로 번역
     */
    private Object translateReviews(JsonNode reviews) {
        try {
            List<Map<String, Object>> translatedReviews = new ArrayList<>();
            
            for (JsonNode review : reviews) {
                Map<String, Object> translatedReview = new HashMap<>();
                
                // 작성자 이름
                if (review.has("author_name")) {
                    translatedReview.put("author_name", review.get("author_name").asText());
                }
                
                // 리뷰 텍스트 (간단한 번역)
                if (review.has("text")) {
                    String originalText = review.get("text").asText();
                    String translatedText = translateReviewText(originalText);
                    translatedReview.put("text", translatedText);
                }
                
                // 평점
                if (review.has("rating")) {
                    translatedReview.put("rating", review.get("rating").asInt());
                }
                
                translatedReviews.add(translatedReview);
            }
            
            return translatedReviews;
        } catch (Exception e) {
            log.error("리뷰 번역 중 오류 발생", e);
            return reviews;
        }
    }
    
    /**
     * 리뷰 텍스트를 한국어로 번역 (간단한 키워드 번역)
     */
    private String translateReviewText(String text) {
        // 간단한 키워드 번역 (실제로는 Google Translate API를 사용하는 것이 좋습니다)
        String translated = text
            .replace("delicious", "맛있는")
            .replace("excellent", "훌륭한")
            .replace("great", "좋은")
            .replace("amazing", "놀라운")
            .replace("wonderful", "훌륭한")
            .replace("fantastic", "환상적인")
            .replace("perfect", "완벽한")
            .replace("good", "좋은")
            .replace("bad", "나쁜")
            .replace("terrible", "끔찍한")
            .replace("awful", "끔찍한")
            .replace("horrible", "끔찍한")
            .replace("food", "음식")
            .replace("service", "서비스")
            .replace("atmosphere", "분위기")
            .replace("restaurant", "식당")
            .replace("cafe", "카페")
            .replace("coffee", "커피")
            .replace("pizza", "피자")
            .replace("pasta", "파스타")
            .replace("noodles", "면")
            .replace("soup", "국")
            .replace("salad", "샐러드")
            .replace("dessert", "디저트")
            .replace("recommend", "추천")
            .replace("highly recommended", "강력 추천")
            .replace("must try", "꼭 드셔보세요")
            .replace("will come back", "다시 올게요")
            .replace("love", "사랑해요")
            .replace("like", "좋아해요")
            .replace("dislike", "싫어해요")
            .replace("hate", "싫어해요");
            
        return translated;
    }

    /**
     * 장소 정보를 한국어로 포맷팅
     */
    public String formatPlaceInfo(Map<String, Object> placeDetails, List<Map<String, Object>> nearbyPlaces) {
        StringBuilder result = new StringBuilder();

        // 주요 장소 정보
        if (!placeDetails.isEmpty()) {
            result.append("📍 ").append(placeDetails.get("name")).append("\n");
            
            if (placeDetails.containsKey("rating") && (Double) placeDetails.get("rating") > 0) {
                result.append("⭐ 평점: ").append(placeDetails.get("rating")).append("/5.0\n");
            }
            
            if (placeDetails.containsKey("formatted_address")) {
                result.append("📍 주소: ").append(placeDetails.get("formatted_address")).append("\n");
            }

            // 영업시간 정보
            if (placeDetails.containsKey("opening_hours")) {
                Object openingHoursObj = placeDetails.get("opening_hours");
                if (openingHoursObj instanceof JsonNode) {
                    JsonNode openingHours = (JsonNode) openingHoursObj;
                    if (openingHours.has("weekday_text")) {
                        result.append("🕒 영업시간:\n");
                        for (JsonNode dayText : openingHours.get("weekday_text")) {
                            result.append("   ").append(dayText.asText()).append("\n");
                        }
                    }
                } else if (openingHoursObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> openingHours = (Map<String, Object>) openingHoursObj;
                    if (openingHours.containsKey("weekday_text")) {
                        result.append("🕒 영업시간:\n");
                        @SuppressWarnings("unchecked")
                        List<String> weekdayText = (List<String>) openingHours.get("weekday_text");
                        for (String dayText : weekdayText) {
                            result.append("   ").append(dayText).append("\n");
                        }
                    }
                }
            }

            // 리뷰 정보
            if (placeDetails.containsKey("reviews")) {
                Object reviewsObj = placeDetails.get("reviews");
                if (reviewsObj instanceof JsonNode) {
                    JsonNode reviews = (JsonNode) reviewsObj;
                    if (reviews.isArray() && reviews.size() > 0) {
                        result.append("💬 최근 리뷰:\n");
                        int reviewCount = Math.min(3, reviews.size());
                        for (int i = 0; i < reviewCount; i++) {
                            JsonNode review = reviews.get(i);
                            result.append("   ").append(review.get("author_name").asText())
                                  .append(": ").append(review.get("text").asText()).append("\n");
                        }
                    }
                } else if (reviewsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> reviews = (List<Map<String, Object>>) reviewsObj;
                    if (!reviews.isEmpty()) {
                        result.append("💬 최근 리뷰:\n");
                        int reviewCount = Math.min(3, reviews.size());
                        for (int i = 0; i < reviewCount; i++) {
                            Map<String, Object> review = reviews.get(i);
                            result.append("   ").append(review.get("author_name").toString())
                                  .append(": ").append(review.get("text").toString()).append("\n");
                        }
                    }
                }
            }
        }

        // 근처 장소 정보
        if (!nearbyPlaces.isEmpty()) {
            result.append("\n🔍 근처 추천 장소:\n");
            for (int i = 0; i < Math.min(5, nearbyPlaces.size()); i++) {
                Map<String, Object> place = nearbyPlaces.get(i);
                result.append("• ").append(place.get("name"));
                if (place.containsKey("rating") && (Double) place.get("rating") > 0) {
                    result.append(" (⭐ ").append(place.get("rating")).append(")");
                }
                result.append("\n");
            }
        }

        return result.toString();
    }
}
