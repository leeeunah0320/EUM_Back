package com.eum.service;

import com.eum.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final SpeechToTextService speechToTextService;
    private final GeminiService geminiService;
    private final GooglePlacesService googlePlacesService;
    private final TtsService ttsService;

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
            ChatbotResponseData responseData = processByIntent(intent, processedQuery, userMessage);

            // 7. 응답 생성
            return ChatbotResponse.builder()
                    .success(true)
                    .message(responseData.getMessage())
                    .processedQuery(processedQuery)
                    .intent(intent)
                    .confidence("high")
                    .sessionId(request.getSessionId())
                    .ttsResponse(responseData.getTtsResponse())
                    .placesResponse(responseData.getPlacesResponse())
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
    private ChatbotResponseData processByIntent(String intent, String processedQuery, String originalQuery) {
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
            return ChatbotResponseData.builder()
                    .message("죄송합니다. 요청을 처리하는 중 오류가 발생했습니다.")
                    .build();
        }
    }

    /**
     * 장소 검색 처리 (TTS 포함)
     */
    private ChatbotResponseData handlePlaceSearch(String query) {
        try {
            log.info("장소 검색 처리: {}", query);
            
            // 장소 검색 요청 생성
            PlacesSearchRequest searchRequest = new PlacesSearchRequest();
            searchRequest.setQuery(query);
            searchRequest.setLanguage("ko");
            searchRequest.setMaxResults(5);
            
            // 장소 검색 실행
            Mono<PlacesSearchResponse> searchResponse = googlePlacesService.searchPlaces(searchRequest);
            PlacesSearchResponse response = searchResponse.block();
            
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                String noResultMessage = "죄송합니다. '" + query + "'에 대한 장소를 찾을 수 없습니다.";
                TtsResponse ttsResponse = ttsService.convertTextToSpeech(
                    new TtsRequest(noResultMessage)
                );
                
                return ChatbotResponseData.builder()
                        .message(noResultMessage)
                        .ttsResponse(ttsResponse)
                        .build();
            }
            
            // 간결한 메시지 생성
            String message = query + " 검색 결과입니다:\n\n";
            message += googlePlacesService.formatMultiplePlacesInfo(response);
            
            // TTS용 텍스트에서 마크다운 형식 제거
            String ttsText = removeMarkdownFormatting(message);
            
            // TTS 변환
            TtsResponse ttsResponse = ttsService.convertTextToSpeech(
                new TtsRequest(ttsText)
            );
            
            log.info("장소 검색 완료: {}개 장소 발견", response.getResults().size());
            
            return ChatbotResponseData.builder()
                    .message(message + "\n\n🔊 음성 안내도 함께 제공됩니다.")
                    .ttsResponse(ttsResponse)
                    .placesResponse(response)
                    .build();

        } catch (Exception e) {
            log.error("장소 검색 처리 중 오류 발생", e);
            String errorMessage = "장소 검색 중 오류가 발생했습니다.";
            TtsResponse ttsResponse = ttsService.convertTextToSpeech(
                new TtsRequest(errorMessage)
            );
            
            return ChatbotResponseData.builder()
                    .message(errorMessage)
                    .ttsResponse(ttsResponse)
                    .build();
        }
    }

    /**
     * 정보 요청 처리 (TTS 포함)
     */
    private ChatbotResponseData handleInformationRequest(String query) {
        try {
            log.info("정보 요청 처리: {}", query);
            
            // Gemini를 통한 정보 제공
            String response = geminiService.sendQueryToGemini(query);
            
            // TTS용 텍스트에서 마크다운 형식 제거
            String ttsText = removeMarkdownFormatting(response);
            
            // TTS 변환
            TtsResponse ttsResponse = ttsService.convertTextToSpeech(
                new TtsRequest(ttsText)
            );
            
            return ChatbotResponseData.builder()
                    .message(response + "\n\n🔊 음성 안내도 함께 제공됩니다.")
                    .ttsResponse(ttsResponse)
                    .build();

        } catch (Exception e) {
            log.error("정보 요청 처리 중 오류 발생", e);
            String errorMessage = "정보를 제공하는 중 오류가 발생했습니다.";
            TtsResponse ttsResponse = ttsService.convertTextToSpeech(
                new TtsRequest(errorMessage)
            );
            
            return ChatbotResponseData.builder()
                    .message(errorMessage)
                    .ttsResponse(ttsResponse)
                    .build();
        }
    }

    /**
     * 일반 대화 처리 (TTS 포함)
     */
    private ChatbotResponseData handleGeneralChat(String query) {
        try {
            log.info("일반 대화 처리: {}", query);
            
            // Gemini를 통한 일반 대화
            String response = geminiService.sendQueryToGemini(query);
            
            // TTS용 텍스트에서 마크다운 형식 제거
            String ttsText = removeMarkdownFormatting(response);
            
            // TTS 변환
            TtsResponse ttsResponse = ttsService.convertTextToSpeech(
                new TtsRequest(ttsText)
            );
            
            return ChatbotResponseData.builder()
                    .message(response + "\n\n🔊 음성 안내도 함께 제공됩니다.")
                    .ttsResponse(ttsResponse)
                    .build();

        } catch (Exception e) {
            log.error("일반 대화 처리 중 오류 발생", e);
            String errorMessage = "대화를 처리하는 중 오류가 발생했습니다.";
            TtsResponse ttsResponse = ttsService.convertTextToSpeech(
                new TtsRequest(errorMessage)
            );
            
            return ChatbotResponseData.builder()
                    .message(errorMessage)
                    .ttsResponse(ttsResponse)
                    .build();
        }
    }

    /**
     * 알 수 없는 의도 처리 (TTS 포함)
     */
    private ChatbotResponseData handleUnknownIntent(String query) {
        try {
            log.info("알 수 없는 의도 처리: {}", query);
            
            // 기본적으로 Gemini에 전달
            String response = geminiService.sendQueryToGemini(query);
            
            // TTS용 텍스트에서 마크다운 형식 제거
            String ttsText = removeMarkdownFormatting(response);
            
            // TTS 변환
            TtsResponse ttsResponse = ttsService.convertTextToSpeech(
                new TtsRequest(ttsText)
            );
            
            return ChatbotResponseData.builder()
                    .message(response + "\n\n🔊 음성 안내도 함께 제공됩니다.")
                    .ttsResponse(ttsResponse)
                    .build();

        } catch (Exception e) {
            log.error("알 수 없는 의도 처리 중 오류 발생", e);
            String errorMessage = "죄송합니다. 요청을 이해할 수 없습니다. 다른 방식으로 질문해주세요.";
            TtsResponse ttsResponse = ttsService.convertTextToSpeech(
                new TtsRequest(errorMessage)
            );
            
            return ChatbotResponseData.builder()
                    .message(errorMessage)
                    .ttsResponse(ttsResponse)
                    .build();
        }
    }

    /**
     * 마크다운 형식 제거 (TTS용)
     */
    private String removeMarkdownFormatting(String text) {
        if (text == null) return "";
        
        // **볼드** 제거
        text = text.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
        
        // *이탤릭* 제거
        text = text.replaceAll("\\*(.*?)\\*", "$1");
        
        // `코드` 제거
        text = text.replaceAll("`(.*?)`", "$1");
        
        // ### 제목 제거
        text = text.replaceAll("^###\\s*", "");
        text = text.replaceAll("^##\\s*", "");
        text = text.replaceAll("^#\\s*", "");
        
        // 링크 [텍스트](URL) 제거
        text = text.replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1");
        
        // 리스트 마커 제거
        text = text.replaceAll("^\\s*[-*+]\\s*", "");
        text = text.replaceAll("^\\s*\\d+\\.\\s*", "");
        
        // 여러 줄바꿈을 하나로
        text = text.replaceAll("\\n\\s*\\n", "\n");
        
        return text.trim();
    }

    /**
     * 서비스 상태 확인
     */
    public boolean isServiceAvailable() {
        return geminiService.isApiKeyValid();
    }
} 
