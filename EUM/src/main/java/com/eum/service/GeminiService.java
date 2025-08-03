package com.eum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiService {

    @Value("${ai.gemini.api-key}")
    private String apiKey;

    @Value("${ai.gemini.model}")
    private String model;

    @Value("${ai.gemini.max-tokens}")
    private int maxTokens;

    @Value("${ai.gemini.temperature}")
    private double temperature;

    private final WebClient webClient;
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1/models/";

    public GeminiService() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Gemini API에 쿼리 전송
     */
    public String sendQueryToGemini(String query) {
        try {
            log.info("Gemini API에 쿼리 전송: {}", query);

            // Gemini API 요청 구조 생성
            Map<String, Object> request = new HashMap<>();
            
            // contents 구조
            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(Map.of("text", query)));
            
            request.put("contents", List.of(content));
            
            // generationConfig 구조
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("maxOutputTokens", maxTokens);
            generationConfig.put("temperature", temperature);
            generationConfig.put("topP", 0.8);
            generationConfig.put("topK", 40);
            
            request.put("generationConfig", generationConfig);

            // API 호출
            Map<String, Object> response = webClient.post()
                    .uri(GEMINI_API_URL + model + ":generateContent?key=" + apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("candidates")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                
                if (!candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    Map<String, Object> contentResponse = (Map<String, Object>) candidate.get("content");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) contentResponse.get("parts");
                    
                    if (!parts.isEmpty()) {
                        String result = (String) parts.get(0).get("text");
                        log.info("Gemini API 응답: {}", result);
                        return result;
                    }
                }
            }
            
            log.warn("Gemini API에서 유효한 응답을 받지 못했습니다.");
            return "죄송합니다. 응답을 생성할 수 없습니다.";

        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생", e);
            return "죄송합니다. 서비스에 일시적인 문제가 발생했습니다.";
        }
    }

    /**
     * 사용자 쿼리 전처리 (의도 분석 및 개선)
     */
    public String preprocessQuery(String userQuery) {
        try {
            String preprocessingPrompt = String.format(
                "다음 사용자 쿼리를 분석하고 개선해주세요. " +
                "장소 검색, 정보 요청, 일반 대화 등을 구분하여 명확하고 구체적인 쿼리로 변환해주세요. " +
                "응답은 개선된 쿼리만 반환해주세요.\n\n" +
                "사용자 쿼리: %s",
                userQuery
            );

            String processedQuery = sendQueryToGemini(preprocessingPrompt);
            log.info("쿼리 전처리 결과: {} -> {}", userQuery, processedQuery);
            return processedQuery;

        } catch (Exception e) {
            log.error("쿼리 전처리 중 오류 발생", e);
            return userQuery; // 전처리 실패 시 원본 쿼리 반환
        }
    }

    /**
     * 사용자 의도 분석
     */
    public String analyzeIntent(String userQuery) {
        try {
            String intentAnalysisPrompt = String.format(
                "다음 사용자 쿼리의 의도를 분석해주세요. " +
                "가능한 의도: PLACE_SEARCH(장소 검색), INFORMATION_REQUEST(정보 요청), GENERAL_CHAT(일반 대화), UNKNOWN(알 수 없음)\n\n" +
                "응답은 의도만 반환해주세요 (예: PLACE_SEARCH)\n\n" +
                "사용자 쿼리: %s",
                userQuery
            );

            String intent = sendQueryToGemini(intentAnalysisPrompt);
            log.info("의도 분석 결과: {} -> {}", userQuery, intent);
            return intent;

        } catch (Exception e) {
            log.error("의도 분석 중 오류 발생", e);
            return "UNKNOWN";
        }
    }

    /**
     * API 키 유효성 검사
     */
    public boolean isApiKeyValid() {
        return apiKey != null && !apiKey.equals("your-gemini-api-key-here") && !apiKey.trim().isEmpty();
    }
} 
