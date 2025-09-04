package com.eum.service;

import com.eum.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final SpeechToTextService speechToTextService;
    private final BedrockService bedrockService;
    private final InputPreprocessingService inputPreprocessingService;

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
            log.info("사용자 의도 분석: {}", intent);

            // 6. 의도에 따른 처리
            String responseMessage = processByIntent(intent, extractedInfo, userMessage);

            // 7. 응답 생성
            return ChatbotResponse.builder()
                    .success(true)
                    .message(responseMessage)
                    .processedQuery(extractedInfo.getProcessedQuery())
                    .intent(intent)
                    .confidence("high")
                    .sessionId(request.getSessionId())
                    .extractedInfo(extractedInfo)
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
     * 장소 검색 처리
     */
    private String handlePlaceSearch(ExtractedInfo extractedInfo, String originalQuery) {
        try {
            log.info("장소 검색 요청 처리: {}", originalQuery);

            // 추출된 정보를 기반으로 검색 쿼리 생성
            String searchQuery = inputPreprocessingService.generateSearchQuery(extractedInfo);
            
            // Bedrock을 사용한 장소 검색 응답 생성
            String placeSearchPrompt = String.format(
                "사용자가 '%s'에 대해 장소를 검색하고 있습니다. " +
                "추출된 위치: %s, 키워드: %s " +
                "이 정보를 바탕으로 도움이 되는 장소 추천 응답을 생성해주세요. " +
                "간결하고 실용적인 정보를 제공해주세요.",
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
            
            // 전체 서비스 상태
            boolean allServicesUp = (Boolean) status.get("bedrock") && 
                                  (Boolean) status.get("stt") && 
                                  (Boolean) status.get("preprocessing");
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


