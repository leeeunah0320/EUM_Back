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
     * 서비스 상태 확인 엔드포인트
     */
    @GetMapping("/status")
    public ResponseEntity<Object> getServiceStatus() {
        try {
            boolean isAvailable = chatbotService.isServiceAvailable();
            
            return ResponseEntity.ok(Map.of(
                "available", isAvailable,
                "message", isAvailable ? "서비스가 정상적으로 작동 중입니다." : "서비스 설정이 완료되지 않았습니다."
            ));

        } catch (Exception e) {
            log.error("서비스 상태 확인 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "available", false,
                "message", "서비스 상태 확인 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 텍스트만으로 챗봇과 대화
     */
    @PostMapping("/text")
    public ResponseEntity<ChatbotResponse> textChat(@RequestBody ChatbotRequest request) {
        try {
            log.info("텍스트 챗봇 요청 수신: message={}", request.getMessage());

            // 세션 ID가 없으면 생성
            if (request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
                request.setSessionId(UUID.randomUUID().toString());
            }

            // 오디오 데이터는 무시하고 텍스트만 처리
            request.setAudioData(null);

            ChatbotResponse response = chatbotService.processChatbotRequest(request);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("텍스트 챗봇 요청 처리 중 오류 발생", e);
            
            ChatbotResponse errorResponse = ChatbotResponse.builder()
                    .success(false)
                    .errorMessage("텍스트 처리 중 오류가 발생했습니다.")
                    .sessionId(request.getSessionId())
                    .build();

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 오디오 데이터로 챗봇과 대화
     */
    @PostMapping("/voice")
    public ResponseEntity<ChatbotResponse> voiceChat(@RequestBody ChatbotRequest request) {
        try {
            log.info("음성 챗봇 요청 수신: sessionId={}", request.getSessionId());

            // 세션 ID가 없으면 생성
            if (request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
                request.setSessionId(UUID.randomUUID().toString());
            }

            // 텍스트 메시지는 무시하고 오디오 데이터만 처리
            request.setMessage(null);

            ChatbotResponse response = chatbotService.processChatbotRequest(request);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("음성 챗봇 요청 처리 중 오류 발생", e);
            
            ChatbotResponse errorResponse = ChatbotResponse.builder()
                    .success(false)
                    .errorMessage("음성 처리 중 오류가 발생했습니다.")
                    .sessionId(request.getSessionId())
                    .build();

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
} 
