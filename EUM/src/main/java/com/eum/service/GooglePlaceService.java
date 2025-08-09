package com.eum.service;

import com.eum.dto.PlacesSearchRequest;
import com.eum.dto.PlacesSearchResponse;
import com.eum.dto.PlaceDetailRequest;
import com.eum.dto.PlaceDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GooglePlacesService {

    @Value("${ai.google-places.api-key}")
    private String apiKey;

    @Value("${ai.google-places.base-url}")
    private String baseUrl;

    private final WebClient webClient;

    /**
     * Google Places API Text Search 수행
     */
    public Mono<PlacesSearchResponse> searchPlaces(PlacesSearchRequest request) {
        Map<String, String> params = new HashMap<>();
        params.put("query", request.getQuery());
        params.put("key", apiKey);
        params.put("language", request.getLanguage() != null ? request.getLanguage() : "ko");
        
        if (request.getLocation() != null) {
            params.put("location", request.getLocation());
        }
        
        if (request.getLatitude() != null && request.getLongitude() != null) {
            params.put("location", request.getLatitude() + "," + request.getLongitude());
        }
        
        if (request.getRadius() != null) {
            params.put("radius", request.getRadius().toString());
        }
        
        if (request.getType() != null) {
            params.put("type", request.getType());
        }

        String url = buildUrl("/textsearch/json", params);
        
        log.info("Google Places API 호출: {}", url);
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(PlacesSearchResponse.class)
                .doOnSuccess(response -> log.info("Places 검색 결과: {}개 장소", 
                    response.getResults() != null ? response.getResults().size() : 0))
                .doOnError(error -> log.error("Places 검색 실패: {}", error.getMessage()));
    }

    /**
     * Google Places API Place Details 조회
     */
    public Mono<PlaceDetailResponse> getPlaceDetails(PlaceDetailRequest request) {
        Map<String, String> params = new HashMap<>();
        params.put("place_id", request.getPlaceId());
        params.put("key", apiKey);
        params.put("language", request.getLanguage() != null ? request.getLanguage() : "ko");
        
        if (request.getFields() != null) {
            params.put("fields", request.getFields());
            } else {
            // 기본 필드들
            params.put("fields", "place_id,name,formatted_address,formatted_phone_number," +
                    "international_phone_number,geometry,types,rating,user_ratings_total," +
                    "opening_hours,business_status,icon,icon_background_color,icon_mask_base_uri," +
                    "photos,price_level,vicinity,website,url,reviews,weekday_text,utc_offset," +
                    "adr_address,plus_code,reference,scope");
        }

        String url = buildUrl("/details/json", params);
        
        log.info("Google Places Details API 호출: {}", url);
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(PlaceDetailResponse.class)
                .doOnSuccess(response -> log.info("Place 상세 정보 조회 완료: {}", 
                    response.getResult() != null ? response.getResult().getName() : "N/A"))
                .doOnError(error -> log.error("Place 상세 정보 조회 실패: {}", error.getMessage()));
    }

    /**
     * URL 파라미터 빌드
     */
    private String buildUrl(String path, Map<String, String> params) {
        StringBuilder url = new StringBuilder(baseUrl + path + "?");
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                url.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }
        
        // 마지막 & 제거
        if (url.charAt(url.length() - 1) == '&') {
            url.setLength(url.length() - 1);
        }
        
        return url.toString();
    }

    /**
     * 장소 정보를 자연어로 변환
     */
    public String formatPlaceInfo(PlacesSearchResponse.PlaceResult place) {
        StringBuilder info = new StringBuilder();
        
        info.append("**").append(place.getName()).append("**\n\n");
        
        if (place.getFormattedAddress() != null) {
            info.append("📍 **주소:** ").append(place.getFormattedAddress()).append("\n");
        }
        
        if (place.getRating() != null) {
            info.append("⭐ **평점:** ").append(place.getRating()).append("/5");
            if (place.getUserRatingsTotal() != null) {
                info.append(" (총 ").append(place.getUserRatingsTotal()).append("개 평가)");
            }
            
            // 평점에 따른 별점 표시
            double rating = place.getRating();
            if (rating >= 4.5) {
                info.append(" - 매우 좋음");
            } else if (rating >= 4.0) {
                info.append(" - 좋음");
            } else if (rating >= 3.5) {
                info.append(" - 보통");
            } else if (rating >= 3.0) {
                info.append(" - 보통 이하");
            } else {
                info.append(" - 낮음");
            }
            info.append("\n");
        }
        
        if (place.getOpeningHours() != null) {
            if (place.getOpeningHours().getOpenNow() != null) {
                info.append("🕒 **현재 영업 상태:** ").append(place.getOpeningHours().getOpenNow() ? "🟢 영업중" : "🔴 영업종료").append("\n");
            }
            
            if (place.getOpeningHours().getWeekdayText() != null) {
                info.append("📅 **영업시간:**\n");
                for (String weekday : place.getOpeningHours().getWeekdayText()) {
                    info.append("  ").append(weekday).append("\n");
                }
            }
        }
        
        // 가격대 정보가 있으면 표시
        if (place.getPriceLevel() != null) {
            String priceLevel = "";
            switch (place.getPriceLevel()) {
                case "0": priceLevel = "무료"; break;
                case "1": priceLevel = "저렴"; break;
                case "2": priceLevel = "보통"; break;
                case "3": priceLevel = "비싼"; break;
                case "4": priceLevel = "매우 비싼"; break;
                default: priceLevel = "정보 없음"; break;
            }
            info.append("💰 **가격대:** ").append(priceLevel).append("\n");
        }
        
        if (place.getVicinity() != null) {
            info.append("🏘️ **근처:** ").append(place.getVicinity()).append("\n");
        }
        
        return info.toString();
    }

    /**
     * 여러 장소 정보를 자연어로 변환
     */
    public String formatMultiplePlacesInfo(PlacesSearchResponse response) {
        if (response.getResults() == null || response.getResults().isEmpty()) {
            return "검색 결과가 없습니다.";
        }
        
        StringBuilder info = new StringBuilder();
        info.append("검색 결과 ").append(response.getResults().size()).append("개 장소를 찾았습니다:\n\n");
        
        for (int i = 0; i < Math.min(response.getResults().size(), 5); i++) {
            PlacesSearchResponse.PlaceResult place = response.getResults().get(i);
            info.append(i + 1).append(". **").append(place.getName()).append("**");
            
            // 평점 정보를 더 자세하게 표시
            if (place.getRating() != null) {
                info.append("\n   ⭐ 평점: ").append(place.getRating()).append("/5");
                if (place.getUserRatingsTotal() != null) {
                    info.append(" (총 ").append(place.getUserRatingsTotal()).append("개 평가)");
                }
                
                // 평점에 따른 별점 표시
                double rating = place.getRating();
                if (rating >= 4.5) {
                    info.append(" - 매우 좋음");
                } else if (rating >= 4.0) {
                    info.append(" - 좋음");
                } else if (rating >= 3.5) {
                    info.append(" - 보통");
                } else if (rating >= 3.0) {
                    info.append(" - 보통 이하");
                } else {
                    info.append(" - 낮음");
                }
            }
            
            if (place.getFormattedAddress() != null) {
                info.append("\n   📍 주소: ").append(place.getFormattedAddress());
            }
            
            if (place.getOpeningHours() != null && place.getOpeningHours().getOpenNow() != null) {
                info.append("\n   🕒 상태: ").append(place.getOpeningHours().getOpenNow() ? "🟢 영업중" : "🔴 영업종료");
            }
            
            // 가격대 정보가 있으면 표시
            if (place.getPriceLevel() != null) {
                String priceLevel = "";
                switch (place.getPriceLevel()) {
                    case "0": priceLevel = "무료"; break;
                    case "1": priceLevel = "저렴"; break;
                    case "2": priceLevel = "보통"; break;
                    case "3": priceLevel = "비싼"; break;
                    case "4": priceLevel = "매우 비싼"; break;
                    default: priceLevel = "정보 없음"; break;
                }
                info.append("\n   💰 가격대: ").append(priceLevel);
            }
            
            info.append("\n\n");
        }
        
        return info.toString();
    }
} 
