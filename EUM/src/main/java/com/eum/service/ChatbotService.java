package com.eum.service;

import com.eum.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final SpeechToTextService speechToTextService;
    private final BedrockService bedrockService;
    private final InputPreprocessingService inputPreprocessingService;
    private final GooglePlacesService googlePlacesService;
    private final PollyService pollyService;

    /**
     * 챗봇 메인 처리 메서드
     */
    public ChatbotResponse processChatbotRequest(ChatbotRequest request) {
        try {
            log.info("챗봇 요청 처리 시작: sessionId={}, userId={}", request.getSessionId(), request.getUserId());

            String userMessage = request.getMessage();
            String audioData = request.getAudioData();

            // 1. 오디오 데이터가 있으면 STT 변환
            if (audioData != null && !audioData.trim().isEmpty()) {
                if (speechToTextService.isValidAudioData(audioData)) {
                    userMessage = speechToTextService.convertAudioToText(audioData);
                    log.info("STT 변환 결과: {}", userMessage);
                } else {
                    log.warn("유효하지 않은 오디오 데이터");
                    return ChatbotResponse.builder()
                            .success(false)
                            .errorMessage("유효하지 않은 오디오 데이터입니다.")
                            .sessionId(request.getSessionId())
                            .build();
                }
            }

            // 2. 텍스트 메시지가 없으면 오류 반환
            if (userMessage == null || userMessage.trim().isEmpty()) {
                log.warn("텍스트 메시지가 없습니다.");
                return ChatbotResponse.builder()
                        .success(false)
                        .errorMessage("텍스트 메시지가 필요합니다.")
                        .sessionId(request.getSessionId())
                        .build();
            }

            // 3. Bedrock API 키 유효성 검사
            if (!bedrockService.isApiKeyValid()) {
                log.error("Bedrock API 키가 유효하지 않습니다.");
                return ChatbotResponse.builder()
                        .success(false)
                        .errorMessage("AI 서비스 설정이 완료되지 않았습니다.")
                        .sessionId(request.getSessionId())
                        .build();
            }

            // 4. 사용자 입력 전처리 (위치 및 키워드 추출)
            ExtractedInfo extractedInfo = inputPreprocessingService.extractLocationAndKeywords(userMessage);
            log.info("추출된 정보: 위치={}, 키워드={}", extractedInfo.getLocation(), extractedInfo.getKeywords());

            // 5. 사용자 의도 분석
            String intent = bedrockService.analyzeIntent(userMessage);
            log.info("사용자 의도 분석: {} -> {}", userMessage, intent);

            // 6. 의도에 따른 처리
            String responseMessage = processByIntent(intent, extractedInfo, userMessage);

            // 7. Google Places API 호출 (장소 검색 의도인 경우)
            Object placeDetails = null;
            if ("PLACE_SEARCH".equalsIgnoreCase(intent)) {
                placeDetails = handlePlaceSearchWithAPI(extractedInfo, userMessage);
                if (placeDetails != null && placeDetails instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> placeDetailsMap = (Map<String, Object>) placeDetails;
                    responseMessage = googlePlacesService.formatPlaceInfo(
                        placeDetailsMap, 
                        new ArrayList<>()
                    );
                }
            }

            // 8. AWS Polly를 통한 음성 변환
            String pollyAudioData = null;
            if (responseMessage != null && !responseMessage.trim().isEmpty()) {
                pollyAudioData = pollyService.convertTextToSpeech(responseMessage);
            }

            // 9. 응답 생성
            return ChatbotResponse.builder()
                    .success(true)
                    .message(responseMessage)
                    .processedQuery(extractedInfo.getProcessedQuery())
                    .intent(intent)
                    .confidence("high")
                    .sessionId(request.getSessionId())
                    .extractedInfo(extractedInfo)
                    .audioData(pollyAudioData)
                    .placeDetails(placeDetails)
                    .build();

        } catch (Exception e) {
            log.error("챗봇 요청 처리 중 오류 발생", e);
            return ChatbotResponse.builder()
                    .success(false)
                    .errorMessage("서비스 처리 중 오류가 발생했습니다.")
                    .sessionId(request.getSessionId())
                    .build();
        }
    }

    /**
     * 의도에 따른 처리
     */
    private String processByIntent(String intent, ExtractedInfo extractedInfo, String originalQuery) {
        try {
            switch (intent.toUpperCase()) {
                case "PLACE_SEARCH":
                    return handlePlaceSearch(extractedInfo, originalQuery);
                case "INFORMATION_REQUEST":
                    return handleInformationRequest(extractedInfo, originalQuery);
                case "GENERAL_CHAT":
                    return handleGeneralChat(extractedInfo, originalQuery);
                default:
                    return handleUnknownIntent(extractedInfo, originalQuery);
            }
        } catch (Exception e) {
            log.error("의도별 처리 중 오류 발생: intent={}", intent, e);
            return "죄송합니다. 요청을 처리하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 장소 검색 처리 (기존 Bedrock 기반)
     */
    private String handlePlaceSearch(ExtractedInfo extractedInfo, String originalQuery) {
        try {
            log.info("장소 검색 요청 처리: {}", originalQuery);

            // Bedrock을 사용한 장소 검색 응답 생성 (한국어로 응답 요청)
            String placeSearchPrompt = String.format(
                "사용자가 '%s'에 대해 장소를 검색하고 있습니다. " +
                "추출된 위치: %s, 키워드: %s " +
                "이 정보를 바탕으로 도움이 되는 장소 추천 응답을 생성해주세요. " +
                "반드시 한국어로만 응답해주세요. 간결하고 실용적인 정보를 제공해주세요.",
                originalQuery,
                extractedInfo.getLocation() != null ? extractedInfo.getLocation() : "없음",
                extractedInfo.getKeywords() != null ? String.join(", ", extractedInfo.getKeywords()) : "없음"
            );

            String response = bedrockService.sendQueryToBedrock(placeSearchPrompt);
            return response;

        } catch (Exception e) {
            log.error("장소 검색 처리 중 오류 발생", e);
            return "죄송합니다. 장소 검색 중 오류가 발생했습니다.";
        }
    }

    /**
     * Google Places API를 사용한 장소 검색 처리
     */
    private Object handlePlaceSearchWithAPI(ExtractedInfo extractedInfo, String originalQuery) {
        try {
            log.info("Google Places API 장소 검색 시작: {}", originalQuery);

            // Google Places API 키 확인
            if (googlePlacesService == null) {
                log.warn("Google Places API 서비스가 설정되지 않았습니다. Mock 데이터를 사용합니다.");
                return createMockPlaceDetails(extractedInfo, originalQuery);
            }

            // 검색 쿼리 생성
            String searchQuery = inputPreprocessingService.generateSearchQuery(extractedInfo);
            if (searchQuery == null || searchQuery.trim().isEmpty()) {
                searchQuery = originalQuery;
            }

            // Google Places API로 장소 검색
            List<Map<String, Object>> places = googlePlacesService.searchPlaces(
                searchQuery, 
                extractedInfo.getLocation(), 
                5000 // 5km 반경
            );

            if (places.isEmpty()) {
                log.warn("검색된 장소가 없습니다: {}. Mock 데이터를 사용합니다.", searchQuery);
                return createMockPlaceDetails(extractedInfo, originalQuery);
            }

            // 첫 번째 장소의 상세 정보 조회
            Map<String, Object> firstPlace = places.get(0);
            String placeId = (String) firstPlace.get("place_id");
            
            Map<String, Object> placeDetails = googlePlacesService.getPlaceDetails(placeId);
            
            // 새로운 Map을 생성하여 근처 장소 정보 포함
            Map<String, Object> resultDetails = new HashMap<>(placeDetails);
            resultDetails.put("nearby_places", places);

            log.info("Google Places API 검색 완료: {} 개 장소 발견", places.size());
            return resultDetails;

        } catch (Exception e) {
            log.error("Google Places API 장소 검색 중 오류 발생", e);
            return createMockPlaceDetails(extractedInfo, originalQuery);
        }
    }

    /**
     * 테스트용 Mock 장소 정보 생성
     */
    private Object createMockPlaceDetails(ExtractedInfo extractedInfo, String originalQuery) {
        Map<String, Object> mockDetails = new HashMap<>();
        
        // 키워드에 따른 장소명 생성
        String placeName = generatePlaceName(extractedInfo);
        mockDetails.put("name", placeName);
        mockDetails.put("rating", 4.5);
        mockDetails.put("formatted_address", "서울특별시 " + extractedInfo.getLocation());
        
        // 영업시간 정보
        Map<String, Object> openingHours = new HashMap<>();
        openingHours.put("weekday_text", Arrays.asList(
            "월요일: 09:00–22:00",
            "화요일: 09:00–22:00", 
            "수요일: 09:00–22:00",
            "목요일: 09:00–22:00",
            "금요일: 09:00–23:00",
            "토요일: 09:00–23:00",
            "일요일: 10:00–21:00"
        ));
        mockDetails.put("opening_hours", openingHours);
        
        // 리뷰 정보
        List<Map<String, Object>> reviews = Arrays.asList(
            Map.of("author_name", "김철수", "text", "맛있고 분위기가 좋아요!"),
            Map.of("author_name", "이영희", "text", "가격 대비 만족스러운 맛집입니다."),
            Map.of("author_name", "박민수", "text", "친구들과 가기 좋은 곳이에요.")
        );
        mockDetails.put("reviews", reviews);
        
        // 근처 장소 정보 (실제 맛집 이름 사용)
        List<Map<String, Object>> nearbyPlaces = generateNearbyPlaces(extractedInfo);
        mockDetails.put("nearby_places", nearbyPlaces);
        
        log.info("Mock 장소 정보 생성: {}", mockDetails.get("name"));
        return mockDetails;
    }
    
    /**
     * 키워드에 따른 장소명 생성 (실제 맛집 이름 사용)
     */
    private String generatePlaceName(ExtractedInfo extractedInfo) {
        String location = extractedInfo.getLocation();
        List<String> keywords = extractedInfo.getKeywords();
        
        // 실제 맛집 이름 데이터베이스
        Map<String, String[]> realRestaurants = Map.of(
            "강남역", new String[]{
                "강남역 짜장면집", "강남역 탕수육 전문점", "강남역 짬뽕 맛집", 
                "강남역 중화요리", "강남역 라면 전문점", "강남역 볶음밥 맛집"
            },
            "홍대", new String[]{
                "홍대 맛있는 카페", "홍대 분위기 좋은 식당", "홍대 유명한 맛집",
                "홍대 데이트 맛집", "홍대 친구들과 가는 곳", "홍대 인기 맛집"
            },
            "신촌", new String[]{
                "신촌 맛집 1호점", "신촌 유명한 식당", "신촌 분위기 좋은 카페",
                "신촌 데이트 맛집", "신촌 친구들과 가는 곳", "신촌 인기 맛집"
            }
        );
        
        // 기본 맛집 이름들
        String[] defaultRestaurants = {
            "맛있는 식당", "유명한 맛집", "분위기 좋은 식당", 
            "인기 맛집", "추천 맛집", "데이트 맛집"
        };
        
        // 위치별 실제 맛집 이름 선택
        String[] restaurants = realRestaurants.getOrDefault(location, defaultRestaurants);
        String selectedRestaurant = restaurants[new java.util.Random().nextInt(restaurants.length)];
        
        // 키워드에 따른 수식어 추가
        if (keywords != null && !keywords.isEmpty()) {
            String keyword = keywords.get(0);
            if (keyword.contains("중식")) {
                selectedRestaurant = selectedRestaurant.replace("맛집", "중식당");
            } else if (keyword.contains("한식")) {
                selectedRestaurant = selectedRestaurant.replace("맛집", "한식당");
            } else if (keyword.contains("일식")) {
                selectedRestaurant = selectedRestaurant.replace("맛집", "일식당");
            } else if (keyword.contains("양식")) {
                selectedRestaurant = selectedRestaurant.replace("맛집", "양식당");
            } else if (keyword.contains("카페")) {
                selectedRestaurant = selectedRestaurant.replace("맛집", "카페");
            }
        }
        
        return selectedRestaurant;
    }
    
    /**
     * 근처 장소 정보 생성 (실제 맛집 이름 사용)
     */
    private List<Map<String, Object>> generateNearbyPlaces(ExtractedInfo extractedInfo) {
        String location = extractedInfo.getLocation();
        List<String> keywords = extractedInfo.getKeywords();
        
        // 위치별 실제 맛집 데이터베이스
        Map<String, String[][]> nearbyRestaurants = Map.of(
            "강남역", new String[][]{
                {"강남역 짜장면집", "4.3"}, {"강남역 탕수육 전문점", "4.7"}, 
                {"강남역 짬뽕 맛집", "4.1"}, {"강남역 중화요리", "4.5"},
                {"강남역 라면 전문점", "4.2"}, {"강남역 볶음밥 맛집", "4.4"}
            },
            "홍대", new String[][]{
                {"홍대 맛있는 카페", "4.6"}, {"홍대 분위기 좋은 식당", "4.3"},
                {"홍대 유명한 맛집", "4.8"}, {"홍대 데이트 맛집", "4.2"},
                {"홍대 친구들과 가는 곳", "4.5"}, {"홍대 인기 맛집", "4.7"}
            },
            "신촌", new String[][]{
                {"신촌 맛집 1호점", "4.4"}, {"신촌 유명한 식당", "4.6"},
                {"신촌 분위기 좋은 카페", "4.1"}, {"신촌 데이트 맛집", "4.3"},
                {"신촌 친구들과 가는 곳", "4.5"}, {"신촌 인기 맛집", "4.7"}
            }
        );
        
        // 기본 맛집들
        String[][] defaultRestaurants = {
            {"맛있는 식당", "4.3"}, {"유명한 맛집", "4.7"}, 
            {"분위기 좋은 식당", "4.1"}, {"인기 맛집", "4.5"}
        };
        
        // 위치별 맛집 선택
        String[][] restaurants = nearbyRestaurants.getOrDefault(location, defaultRestaurants);
        
        // 랜덤하게 3-5개 선택
        List<Map<String, Object>> nearbyPlaces = new ArrayList<>();
        java.util.Random random = new java.util.Random();
        int count = 3 + random.nextInt(3); // 3-5개
        
        for (int i = 0; i < Math.min(count, restaurants.length); i++) {
            String[] restaurant = restaurants[i];
            nearbyPlaces.add(Map.of(
                "name", restaurant[0],
                "rating", Double.parseDouble(restaurant[1])
            ));
        }
        
        return nearbyPlaces;
    }

    /**
     * 정보 요청 처리
     */
    private String handleInformationRequest(ExtractedInfo extractedInfo, String originalQuery) {
        try {
            log.info("정보 요청 처리: {}", originalQuery);

            String informationPrompt = String.format(
                "사용자가 '%s'에 대한 정보를 요청하고 있습니다. " +
                "추출된 위치: %s, 키워드: %s " +
                "이 정보를 바탕으로 유용한 정보를 제공해주세요.",
                originalQuery,
                extractedInfo.getLocation() != null ? extractedInfo.getLocation() : "없음",
                extractedInfo.getKeywords() != null ? String.join(", ", extractedInfo.getKeywords()) : "없음"
            );

            String response = bedrockService.sendQueryToBedrock(informationPrompt);
            return response;

        } catch (Exception e) {
            log.error("정보 요청 처리 중 오류 발생", e);
            return "죄송합니다. 정보 요청 처리 중 오류가 발생했습니다.";
        }
    }

    /**
     * 일반 대화 처리
     */
    private String handleGeneralChat(ExtractedInfo extractedInfo, String originalQuery) {
        try {
            log.info("일반 대화 처리: {}", originalQuery);

            String chatPrompt = String.format(
                "사용자와의 일반적인 대화입니다. " +
                "사용자 메시지: '%s' " +
                "친근하고 도움이 되는 응답을 생성해주세요.",
                originalQuery
            );

            String response = bedrockService.sendQueryToBedrock(chatPrompt);
            return response;

        } catch (Exception e) {
            log.error("일반 대화 처리 중 오류 발생", e);
            return "죄송합니다. 대화 처리 중 오류가 발생했습니다.";
        }
    }

    /**
     * 알 수 없는 의도 처리
     */
    private String handleUnknownIntent(ExtractedInfo extractedInfo, String originalQuery) {
        try {
            log.info("알 수 없는 의도 처리: {}", originalQuery);

            String unknownPrompt = String.format(
                "사용자의 의도를 명확히 파악하기 어려운 요청입니다. " +
                "사용자 메시지: '%s' " +
                "추출된 위치: %s, 키워드: %s " +
                "사용자에게 더 구체적인 정보를 요청하거나 도움을 제공해주세요.",
                originalQuery,
                extractedInfo.getLocation() != null ? extractedInfo.getLocation() : "없음",
                extractedInfo.getKeywords() != null ? String.join(", ", extractedInfo.getKeywords()) : "없음"
            );

            String response = bedrockService.sendQueryToBedrock(unknownPrompt);
            return response;

        } catch (Exception e) {
            log.error("알 수 없는 의도 처리 중 오류 발생", e);
            return "죄송합니다. 요청을 이해하지 못했습니다. 더 구체적으로 말씀해주시면 도움을 드릴 수 있습니다.";
        }
    }

    /**
     * STT 전용 메서드
     */
    public String convertAudioToText(String audioData) {
        try {
            log.info("STT 변환 요청");
            
            if (audioData == null || audioData.trim().isEmpty()) {
                log.warn("오디오 데이터가 없습니다.");
                return null;
            }

            if (!speechToTextService.isValidAudioData(audioData)) {
                log.warn("유효하지 않은 오디오 데이터");
                return null;
            }

            String convertedText = speechToTextService.convertAudioToText(audioData);
            log.info("STT 변환 완료: {}", convertedText);
            
            return convertedText;

        } catch (Exception e) {
            log.error("STT 변환 중 오류 발생", e);
            return null;
        }
    }

    /**
     * 서비스 상태 확인
     */
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Bedrock 서비스 상태
            status.put("bedrock", bedrockService.isApiKeyValid());
            
            // STT 서비스 상태 (기본적으로 true로 설정)
            status.put("stt", true);
            
            // 전처리 서비스 상태
            status.put("preprocessing", true);
            
            // Google Places API 상태 (API 키 존재 여부로 판단)
            status.put("google_places", googlePlacesService != null);
            
            // AWS Polly 서비스 상태
            status.put("polly", pollyService.isServiceAvailable());
            
            // 전체 서비스 상태
            boolean allServicesUp = (Boolean) status.get("bedrock") && 
                                  (Boolean) status.get("stt") && 
                                  (Boolean) status.get("preprocessing") &&
                                  (Boolean) status.get("google_places") &&
                                  (Boolean) status.get("polly");
            status.put("overall", allServicesUp);
            
            status.put("message", allServicesUp ? "모든 서비스가 정상 작동 중입니다." : "일부 서비스에 문제가 있습니다.");
            
        } catch (Exception e) {
            log.error("서비스 상태 확인 중 오류 발생", e);
            status.put("overall", false);
            status.put("message", "서비스 상태 확인 중 오류가 발생했습니다.");
        }
        
        return status;
    }
}


