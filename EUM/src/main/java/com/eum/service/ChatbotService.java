package com.eum.service;

import com.eum.dto.ChatbotRequest;
import com.eum.dto.ChatbotResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final SpeechToTextService speechToTextService;
    private final GeminiService geminiService;
    private final GooglePlacesService googlePlacesService;

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

            // 3. Gemini API 키 유효성 검사
            if (!geminiService.isApiKeyValid()) {
                log.error("Gemini API 키가 유효하지 않습니다.");
                return ChatbotResponse.builder()
                        .success(false)
                        .errorMessage("AI 서비스 설정이 완료되지 않았습니다.")
                        .sessionId(request.getSessionId())
                        .build();
            }

            // 4. 사용자 의도 분석
            String intent = geminiService.analyzeIntent(userMessage);
            log.info("사용자 의도 분석: {}", intent);

            // 5. 쿼리 전처리
            String processedQuery = geminiService.preprocessQuery(userMessage);
            log.info("쿼리 전처리: {} -> {}", userMessage, processedQuery);

            // 6. 의도에 따른 처리
            String responseMessage = processByIntent(intent, processedQuery, userMessage);

            // 7. 응답 생성
            return ChatbotResponse.builder()
                    .success(true)
                    .message(responseMessage)
                    .processedQuery(processedQuery)
                    .intent(intent)
                    .confidence("high")
                    .sessionId(request.getSessionId())
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
    private String processByIntent(String intent, String processedQuery, String originalQuery) {
        try {
            switch (intent.toUpperCase()) {
                case "PLACE_SEARCH":
                    return handlePlaceSearch(processedQuery);
                case "INFORMATION_REQUEST":
                    return handleInformationRequest(processedQuery);
                case "GENERAL_CHAT":
                    return handleGeneralChat(processedQuery);
                default:
                    return handleUnknownIntent(originalQuery);
            }
        } catch (Exception e) {
            log.error("의도별 처리 중 오류 발생: intent={}", intent, e);
            return "죄송합니다. 요청을 처리하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 장소 검색 처리
     */
    private String handlePlaceSearch(String query) {
        try {
            log.info("장소 검색 처리: {}", query);
            
            // Google Places API 키 유효성 검사
            if (!googlePlacesService.isApiKeyValid()) {
                log.warn("Google Places API 키가 유효하지 않습니다.");
                return "장소 검색 서비스를 사용할 수 없습니다. 관리자에게 문의해주세요.";
            }

            // 장소 검색 실행
            String searchResult = googlePlacesService.searchPlaces(query, null);
            
            if (searchResult.contains("검색 결과가 없습니다.")) {
                return "죄송합니다. '" + query + "'에 대한 장소를 찾을 수 없습니다.";
            }
            
            return searchResult;

        } catch (Exception e) {
            log.error("장소 검색 처리 중 오류 발생", e);
            return "장소 검색 중 오류가 발생했습니다.";
        }
    }

    /**
     * 정보 요청 처리
     */
    private String handleInformationRequest(String query) {
        try {
            log.info("정보 요청 처리: {}", query);
            
            // Gemini를 통한 정보 제공
            String response = geminiService.sendQueryToGemini(query);
            return response;

        } catch (Exception e) {
            log.error("정보 요청 처리 중 오류 발생", e);
            return "정보를 제공하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 일반 대화 처리
     */
    private String handleGeneralChat(String query) {
        try {
            log.info("일반 대화 처리: {}", query);
            
            // Gemini를 통한 일반 대화
            String response = geminiService.sendQueryToGemini(query);
            return response;

        } catch (Exception e) {
            log.error("일반 대화 처리 중 오류 발생", e);
            return "대화를 처리하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 알 수 없는 의도 처리
     */
    private String handleUnknownIntent(String query) {
        try {
            log.info("알 수 없는 의도 처리: {}", query);
            
            // 기본적으로 Gemini에 전달
            String response = geminiService.sendQueryToGemini(query);
            return response;

        } catch (Exception e) {
            log.error("알 수 없는 의도 처리 중 오류 발생", e);
            return "죄송합니다. 요청을 이해할 수 없습니다. 다른 방식으로 질문해주세요.";
        }
    }

    /**
     * 서비스 상태 확인
     */
    public boolean isServiceAvailable() {
        return geminiService.isApiKeyValid() && googlePlacesService.isApiKeyValid();
    }
} 
