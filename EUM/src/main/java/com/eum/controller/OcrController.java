package com.eum.controller;

import com.eum.dto.OcrRequest;
import com.eum.dto.OcrResponse;
import com.eum.service.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {
    private final OcrService ocrService;

    @PostMapping("/analyze")
    public ResponseEntity<OcrResponse> analyzeImage(@RequestBody OcrRequest request) {
        System.out.println("=== OCR CONTROLLER START ===");
        System.out.println("Request received, imageBase64 length: " + (request.getImageBase64() != null ? request.getImageBase64().length() : "null"));
        
        try {
            OcrResponse response = ocrService.processImage(request);
            System.out.println("Service response - success: " + response.isSuccess());
            if (!response.isSuccess()) {
                System.out.println("Error message: " + response.getErrorMessage());
            }
            System.out.println("=== OCR CONTROLLER END ===");
            return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            System.err.println("=== OCR CONTROLLER ERROR ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=============================");
            
            OcrResponse errorResponse = new OcrResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("Controller error: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/test")
    public ResponseEntity<OcrResponse> testImage(@RequestBody OcrRequest request) {
        System.out.println("=== OCR TEST ENDPOINT ===");
        System.out.println("Request received, imageBase64 length: " + (request.getImageBase64() != null ? request.getImageBase64().length() : "null"));
        
        try {
            // Base64 디코딩 테스트만
            byte[] imageBytes = java.util.Base64.getDecoder().decode(request.getImageBase64());
            System.out.println("Base64 decoded successfully, image bytes length: " + imageBytes.length);
            
            // 이미지 정보 출력
            System.out.println("Image size: " + imageBytes.length + " bytes");
            System.out.println("Image starts with: " + new String(imageBytes, 0, Math.min(50, imageBytes.length)));
            
            OcrResponse response = new OcrResponse();
            response.setSuccess(true);
            response.setTranslatedText("테스트 성공 - 이미지 크기: " + imageBytes.length + " bytes");
            response.setDetectedLabels(new java.util.ArrayList<>());
            response.setAudioBase64("");
            
            System.out.println("=== OCR TEST SUCCESS ===");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("=== OCR TEST ERROR ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            
            OcrResponse errorResponse = new OcrResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage("Test error: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
} 
