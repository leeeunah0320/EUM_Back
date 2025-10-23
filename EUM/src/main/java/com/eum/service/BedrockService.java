package com.eum.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class BedrockService {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.bedrock.model-id}")
    private String modelId;

    @Value("${aws.bedrock.max-tokens}")
    private int maxTokens;

    @Value("${aws.bedrock.temperature}")
    private double temperature;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Bedrock Titan 모델에 쿼리 전송
     */
    public String sendQueryToBedrock(String query) {
        try {
            log.info("Bedrock Titan 모델에 쿼리 전송: {}", query);

            // Bedrock 클라이언트 생성
            try (BedrockRuntimeClient bedrockClient = BedrockRuntimeClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build()) {

                // 요청 페이로드 생성
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("inputText", query);
                
                Map<String, Object> textGenerationConfig = new HashMap<>();
                textGenerationConfig.put("maxTokenCount", maxTokens);
                textGenerationConfig.put("temperature", temperature);
                textGenerationConfig.put("topP", 0.9);
                
                requestBody.put("textGenerationConfig", textGenerationConfig);

                // JSON 페이로드를 바이트로 변환
                String jsonPayload = objectMapper.writeValueAsString(requestBody);
                SdkBytes body = SdkBytes.fromString(jsonPayload, StandardCharsets.UTF_8);

                // Bedrock 모델 호출 요청 생성
                InvokeModelRequest request = InvokeModelRequest.builder()
                        .modelId(modelId)
                        .body(body)
                        .build();

                // API 호출
                InvokeModelResponse response = bedrockClient.invokeModel(request);
                
                // 응답 파싱
                String responseBody = response.body().asString(StandardCharsets.UTF_8);
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                
                // 결과 추출
                if (jsonResponse.has("results") && jsonResponse.get("results").isArray()) {
                    JsonNode results = jsonResponse.get("results");
                    if (results.size() > 0) {
                        JsonNode firstResult = results.get(0);
                        if (firstResult.has("outputText")) {
                            String result = firstResult.get("outputText").asText();
                            log.info("Bedrock Titan 응답: {}", result);
                            return result;
                        }
                    }
                }
                
                log.warn("Bedrock에서 유효한 응답을 받지 못했습니다.");
                return "죄송합니다. 응답을 생성할 수 없습니다.";

            } catch (Exception e) {
                log.error("Bedrock 클라이언트 생성 중 오류 발생", e);
                throw new RuntimeException("Bedrock 서비스 초기화 실패", e);
            }

        } catch (Exception e) {
            log.error("Bedrock API 호출 중 오류 발생", e);
            return "죄송합니다. AI 서비스에 일시적인 문제가 발생했습니다.";
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

            String processedQuery = sendQueryToBedrock(preprocessingPrompt);
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
                "다음 사용자 쿼리의 의도를 분석해주세요.\n\n" +
                "의도 분류:\n" +
                "- PLACE_SEARCH: 장소 검색, 맛집 추천, 카페 찾기, 호텔 예약, 병원 찾기 등\n" +
                "- INFORMATION_REQUEST: 일반적인 정보 요청, 질문\n" +
                "- GENERAL_CHAT: 인사, 감사, 일반 대화\n" +
                "- UNKNOWN: 의도를 파악하기 어려운 경우\n\n" +
                "예시:\n" +
                "- '강남역 맛집 추천해줘' → PLACE_SEARCH\n" +
                "- '홍대 카페 어디가 좋아?' → PLACE_SEARCH\n" +
                "- '날씨가 어때?' → INFORMATION_REQUEST\n" +
                "- '안녕하세요' → GENERAL_CHAT\n\n" +
                "응답은 반드시 다음 중 하나만 반환하세요: PLACE_SEARCH, INFORMATION_REQUEST, GENERAL_CHAT, UNKNOWN\n\n" +
                "사용자 쿼리: %s",
                userQuery
            );

            String intent = sendQueryToBedrock(intentAnalysisPrompt);
            // 응답에서 의도만 추출 (공백 제거)
            intent = intent.trim().toUpperCase();
            
            // 유효한 의도인지 확인
            if (!intent.equals("PLACE_SEARCH") && !intent.equals("INFORMATION_REQUEST") && 
                !intent.equals("GENERAL_CHAT") && !intent.equals("UNKNOWN")) {
                intent = "UNKNOWN";
            }
            
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
        try {
            DefaultCredentialsProvider.create().resolveCredentials();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
