package com.eum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
public class GooglePlacesService {

    @Value("${ai.google-places.api-key}")
    private String apiKey;

    @Value("${ai.google-places.base-url}")
    private String baseUrl;

    private final WebClient webClient;

    public GooglePlacesService() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 장소 검색
     */
    public String searchPlaces(String query, String location) {
        try {
            log.info("Google Places API 장소 검색: {}", query);

            String searchUrl = baseUrl + "/textsearch/json";
            String fullUrl = searchUrl + "?query=" + encodeQuery(query) + "&key=" + apiKey;

            if (location != null && !location.trim().isEmpty()) {
                fullUrl += "&location=" + encodeQuery(location);
            }

            // API 호출
            Map<String, Object> response = webClient.get()
                    .uri(fullUrl)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && "OK".equals(response.get("status"))) {
                log.info("Google Places API 검색 성공");
                return formatPlacesResponse(response);
            } else {
                log.warn("Google Places API 검색 실패: {}", response != null ? response.get("status") : "null");
                return "장소를 찾을 수 없습니다.";
            }

        } catch (Exception e) {
            log.error("Google Places API 호출 중 오류 발생", e);
            return "장소 검색 중 오류가 발생했습니다.";
        }
    }

    /**
     * 근처 장소 검색
     */
    public String searchNearbyPlaces(String type, String location) {
        try {
            log.info("Google Places API 근처 장소 검색: type={}, location={}", type, location);

            String nearbyUrl = baseUrl + "/nearbysearch/json";
            String fullUrl = nearbyUrl + "?location=" + encodeQuery(location) + 
                           "&radius=5000&type=" + encodeQuery(type) + "&key=" + apiKey;

            // API 호출
            Map<String, Object> response = webClient.get()
                    .uri(fullUrl)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && "OK".equals(response.get("status"))) {
                log.info("Google Places API 근처 검색 성공");
                return formatPlacesResponse(response);
            } else {
                log.warn("Google Places API 근처 검색 실패: {}", response != null ? response.get("status") : "null");
                return "근처에 해당하는 장소를 찾을 수 없습니다.";
            }

        } catch (Exception e) {
            log.error("Google Places API 근처 검색 중 오류 발생", e);
            return "근처 장소 검색 중 오류가 발생했습니다.";
        }
    }

    /**
     * 장소 상세 정보 조회
     */
    public String getPlaceDetails(String placeId) {
        try {
            log.info("Google Places API 장소 상세 정보 조회: {}", placeId);

            String detailsUrl = baseUrl + "/details/json";
            String fullUrl = detailsUrl + "?place_id=" + encodeQuery(placeId) + 
                           "&fields=name,formatted_address,formatted_phone_number,website,rating,opening_hours&key=" + apiKey;

            // API 호출
            Map<String, Object> response = webClient.get()
                    .uri(fullUrl)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && "OK".equals(response.get("status"))) {
                log.info("Google Places API 상세 정보 조회 성공");
                return formatPlaceDetailsResponse(response);
            } else {
                log.warn("Google Places API 상세 정보 조회 실패: {}", response != null ? response.get("status") : "null");
                return "장소 상세 정보를 찾을 수 없습니다.";
            }

        } catch (Exception e) {
            log.error("Google Places API 상세 정보 조회 중 오류 발생", e);
            return "장소 상세 정보 조회 중 오류가 발생했습니다.";
        }
    }

    /**
     * 응답 포맷팅
     */
    private String formatPlacesResponse(Map<String, Object> response) {
        try {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> results = (java.util.List<Map<String, Object>>) response.get("results");
            
            if (results == null || results.isEmpty()) {
                return "검색 결과가 없습니다.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("검색 결과:\n\n");

            for (int i = 0; i < Math.min(results.size(), 5); i++) {
                Map<String, Object> place = results.get(i);
                String name = (String) place.get("name");
                String address = (String) place.get("formatted_address");
                Double rating = (Double) place.get("rating");

                sb.append(i + 1).append(". ").append(name).append("\n");
                if (address != null) {
                    sb.append("   주소: ").append(address).append("\n");
                }
                if (rating != null) {
                    sb.append("   평점: ").append(rating).append("/5.0\n");
                }
                sb.append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("응답 포맷팅 중 오류 발생", e);
            return "검색 결과를 처리하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 상세 정보 응답 포맷팅
     */
    private String formatPlaceDetailsResponse(Map<String, Object> response) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.get("result");
            
            if (result == null) {
                return "상세 정보를 찾을 수 없습니다.";
            }

            StringBuilder sb = new StringBuilder();
            String name = (String) result.get("name");
            String address = (String) result.get("formatted_address");
            String phone = (String) result.get("formatted_phone_number");
            String website = (String) result.get("website");
            Double rating = (Double) result.get("rating");

            sb.append("장소 정보:\n\n");
            sb.append("이름: ").append(name).append("\n");
            
            if (address != null) {
                sb.append("주소: ").append(address).append("\n");
            }
            if (phone != null) {
                sb.append("전화번호: ").append(phone).append("\n");
            }
            if (website != null) {
                sb.append("웹사이트: ").append(website).append("\n");
            }
            if (rating != null) {
                sb.append("평점: ").append(rating).append("/5.0\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("상세 정보 응답 포맷팅 중 오류 발생", e);
            return "상세 정보를 처리하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * URL 인코딩
     */
    private String encodeQuery(String query) {
        try {
            return java.net.URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            log.error("URL 인코딩 중 오류 발생", e);
            return query;
        }
    }

    /**
     * API 키 유효성 검사
     */
    public boolean isApiKeyValid() {
        return apiKey != null && !apiKey.equals("your-google-places-api-key-here") && !apiKey.trim().isEmpty();
    }
} 
