package com.eum.controller;

import com.eum.dto.ChatbotRequest;
import com.eum.dto.ChatbotResponse;
import com.eum.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * 챗봇 메인 엔드포인트
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatbotResponse> chat(@RequestBody ChatbotRequest request) {
        try {
            log.info("챗봇 요청 수신: sessionId={}, userId={}", request.getSessionId(), request.getUserId());

            // 세션 ID가 없으면 생성
            if (request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
                request.setSessionId(UUID.randomUUID().toString());
            }

            // 챗봇 서비스 호출
            ChatbotResponse response = chatbotService.processChatbotRequest(request);

            log.info("챗봇 응답 생성 완료: success={}, intent={}", response.isSuccess(), response.getIntent());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("챗봇 요청 처리 중 오류 발생", e);
            
            ChatbotResponse errorResponse = ChatbotResponse.builder()
                    .success(false)
                    .errorMessage("서비스 처리 중 오류가 발생했습니다.")
                    .sessionId(request.getSessionId())
                    .build();

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * STT 전용 엔드포인트
     */
    @PostMapping("/stt")
    public ResponseEntity<Map<String, Object>> convertSpeechToText(@RequestBody ChatbotRequest request) {
        try {
            log.info("STT 요청 수신: sessionId={}", request.getSessionId());

            String audioData = request.getAudioData();
            if (audioData == null || audioData.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "errorMessage", "오디오 데이터가 필요합니다."));
            }

            // STT 서비스 호출
            String convertedText = chatbotService.convertAudioToText(audioData);
            
            if (convertedText != null && !convertedText.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "text", convertedText,
                        "sessionId", request.getSessionId()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "errorMessage", "STT 변환에 실패했습니다.",
                        "sessionId", request.getSessionId()
                ));
            }

        } catch (Exception e) {
            log.error("STT 요청 처리 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "errorMessage", "STT 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 서비스 상태 확인 엔드포인트
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        try {
            Map<String, Object> status = chatbotService.getServiceStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("서비스 상태 확인 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "서비스 상태를 확인할 수 없습니다."));
        }
    }

    /**
     * 테스트용 엔드포인트
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> test(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            log.info("테스트 요청: {}", message);

            ChatbotRequest chatbotRequest = new ChatbotRequest();
            chatbotRequest.setMessage(message);
            chatbotRequest.setSessionId(UUID.randomUUID().toString());

            ChatbotResponse response = chatbotService.processChatbotRequest(chatbotRequest);

            return ResponseEntity.ok(Map.of(
                    "success", response.isSuccess(),
                    "message", response.getMessage(),
                    "intent", response.getIntent(),
                    "extractedInfo", response.getExtractedInfo()
            ));

        } catch (Exception e) {
            log.error("테스트 요청 처리 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "테스트 처리 중 오류가 발생했습니다."));
        }
    }
}
