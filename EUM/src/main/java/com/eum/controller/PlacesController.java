package com.eum.controller;

import com.eum.dto.*;
import com.eum.service.GooglePlacesService;
import com.eum.service.TtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlacesController {

    private final GooglePlacesService placesService;
    private final TtsService ttsService;

    /**
     * 장소 검색
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchPlaces(@RequestBody PlacesSearchRequest request) {
        try {
            log.info("장소 검색 요청: {}", request.getQuery());

            return placesService.searchPlaces(request)
                    .map(response -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("data", response);
                        result.put("formattedText", placesService.formatMultiplePlacesInfo(response));
                        return ResponseEntity.ok(result);
                    })
                    .defaultIfEmpty(ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", "검색 결과가 없습니다."
                    )))
                    .block();

        } catch (Exception e) {
            log.error("장소 검색 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "장소 검색 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 장소 상세 정보 조회
     */
    @PostMapping("/details")
    public ResponseEntity<Map<String, Object>> getPlaceDetails(@RequestBody PlaceDetailRequest request) {
        try {
            log.info("장소 상세 정보 조회 요청: {}", request.getPlaceId());

            return placesService.getPlaceDetails(request)
                    .map(response -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("data", response);
                        return ResponseEntity.ok(result);
                    })
                    .defaultIfEmpty(ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", "상세 정보를 찾을 수 없습니다."
                    )))
                    .block();

        } catch (Exception e) {
            log.error("장소 상세 정보 조회 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "장소 상세 정보 조회 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * TTS 변환
     */
    @PostMapping("/tts")
    public ResponseEntity<Map<String, Object>> convertToSpeech(@RequestBody TtsRequest request) {
        try {
            log.info("TTS 변환 요청: {}", request.getText().substring(0, Math.min(50, request.getText().length())));

            // 기본값 설정
            if (request.getVoiceId() == null || request.getVoiceId().trim().isEmpty()) {
                request.setVoiceId("Seoyeon");
            }
            if (request.getOutputFormat() == null || request.getOutputFormat().trim().isEmpty()) {
                request.setOutputFormat("mp3");
            }
            if (request.getEngine() == null || request.getEngine().trim().isEmpty()) {
                request.setEngine("neural");
            }
            if (request.getLanguageCode() == null || request.getLanguageCode().trim().isEmpty()) {
                request.setLanguageCode("ko-KR");
            }

            TtsResponse response = ttsService.convertTextToSpeech(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("TTS 변환 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "음성 변환 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 장소 검색 + TTS 변환 (통합)
     */
    @PostMapping("/search-with-tts")
    public ResponseEntity<Map<String, Object>> searchPlacesWithTts(@RequestBody PlacesSearchRequest request) {
        try {
            log.info("장소 검색 + TTS 요청: {}", request.getQuery());

            return placesService.searchPlaces(request)
                    .flatMap(placesResponse -> {
                        String formattedText = placesService.formatMultiplePlacesInfo(placesResponse);
                        
                        // TTS 변환
                        TtsResponse ttsResponse = ttsService.convertMultiplePlacesToSpeech(formattedText);
                        
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("places", placesResponse);
                        result.put("formattedText", formattedText);
                        result.put("tts", ttsResponse);
                        
                        return Mono.just(ResponseEntity.ok(result));
                    })
                    .defaultIfEmpty(ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", "검색 결과가 없습니다."
                    )))
                    .block();

        } catch (Exception e) {
            log.error("장소 검색 + TTS 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "장소 검색 및 음성 변환 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 장소 상세 정보 + TTS 변환 (통합)
     */
    @PostMapping("/details-with-tts")
    public ResponseEntity<Map<String, Object>> getPlaceDetailsWithTts(@RequestBody PlaceDetailRequest request) {
        try {
            log.info("장소 상세 정보 + TTS 요청: {}", request.getPlaceId());

            return placesService.getPlaceDetails(request)
                    .flatMap(detailResponse -> {
                        if (detailResponse.getResult() != null) {
                            String formattedText = placesService.formatPlaceInfo(
                                convertToPlaceResult(detailResponse.getResult())
                            );
                            
                            // TTS 변환
                            TtsResponse ttsResponse = ttsService.convertPlaceInfoToSpeech(formattedText);
                            
                            Map<String, Object> result = new HashMap<>();
                            result.put("success", true);
                            result.put("details", detailResponse);
                            result.put("formattedText", formattedText);
                            result.put("tts", ttsResponse);
                            
                            return Mono.just(ResponseEntity.ok(result));
                        } else {
                            return Mono.just(ResponseEntity.ok(Map.<String, Object>of(
                                    "success", false,
                                    "message", "상세 정보를 찾을 수 없습니다."
                            )));
                        }
                    })
                    .defaultIfEmpty(ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", "상세 정보를 찾을 수 없습니다."
                    )))
                    .block();

        } catch (Exception e) {
            log.error("장소 상세 정보 + TTS 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "장소 상세 정보 조회 및 음성 변환 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 사용 가능한 음성 목록 조회
     */
    @GetMapping("/voices")
    public ResponseEntity<Map<String, Object>> getAvailableVoices() {
        try {
            String voices = ttsService.getAvailableVoices();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("voices", voices);
            
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("음성 목록 조회 중 오류 발생", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "음성 목록 조회 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * PlaceDetail을 PlaceResult로 변환하는 헬퍼 메서드
     */
    private PlacesSearchResponse.PlaceResult convertToPlaceResult(PlaceDetailResponse.PlaceDetail detail) {
        PlacesSearchResponse.PlaceResult result = new PlacesSearchResponse.PlaceResult();
        result.setPlaceId(detail.getPlaceId());
        result.setName(detail.getName());
        result.setFormattedAddress(detail.getFormattedAddress());
        result.setRating(detail.getRating());
        result.setUserRatingsTotal(detail.getUserRatingsTotal());
        // OpeningHours는 타입이 다르므로 변환하지 않음
        result.setVicinity(detail.getVicinity());
        return result;
    }
}
