package com.eum.service;

import com.eum.dto.OcrRequest;
import com.eum.dto.OcrResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.*;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.*;

import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OcrService {
    private final RekognitionClient rekognitionClient;
    private final TranslateClient translateClient;
    private final PollyClient pollyClient;

    @Value("${aws.rekognition.project-version-arn}")
    private String projectVersionArn;
    @Value("${aws.rekognition.min-confidence:70.0}")
    private float minConfidence;

    public OcrResponse processImage(OcrRequest request) {
        try {
            System.out.println("=== OCR SERVICE START ===");
            System.out.println("Project Version ARN: " + projectVersionArn);
            System.out.println("Min Confidence: " + minConfidence);
            
            // 1. Base64 → byte[]
            System.out.println("1. Decoding Base64 image...");
            byte[] imageBytes = Base64.getDecoder().decode(request.getImageBase64());
            System.out.println("Image bytes length: " + imageBytes.length);

            // 2. Rekognition Custom Labels 분석
            System.out.println("2. Calling AWS Rekognition...");
            DetectCustomLabelsRequest rekReq = DetectCustomLabelsRequest.builder()
                .projectVersionArn(projectVersionArn)
                .image(Image.builder().bytes(SdkBytes.fromByteArray(imageBytes)).build())
                .minConfidence(minConfidence)
                .build();
            DetectCustomLabelsResponse rekRes = rekognitionClient.detectCustomLabels(rekReq);
            System.out.println("Rekognition response received");
            System.out.println("Raw custom labels: " + rekRes.customLabels());

                        // 3. 결과 파싱 (영어 레이블 추출)
            System.out.println("3. Parsing results...");
            List<OcrResponse.DetectedLabel> allLabels = rekRes.customLabels().stream().map(label -> {
                OcrResponse.DetectedLabel dto = new OcrResponse.DetectedLabel();
                dto.setName(label.name());
                dto.setConfidence(label.confidence());
                if (label.geometry() != null) dto.setGeometry(label.geometry().toString());
                return dto;
            }).collect(Collectors.toList());
            System.out.println("All detected labels count: " + allLabels.size());
            
            // 4. 신뢰도가 가장 높은 레이블 하나만 선택
            List<OcrResponse.DetectedLabel> labels;
            if (!allLabels.isEmpty()) {
                OcrResponse.DetectedLabel bestLabel = allLabels.stream()
                    .max((a, b) -> Float.compare(a.getConfidence(), b.getConfidence()))
                    .orElse(null);
                labels = List.of(bestLabel);
                System.out.println("Selected best label: " + bestLabel.getName() + " (confidence: " + bestLabel.getConfidence() + "%)");
            } else {
                labels = new ArrayList<>();
                System.out.println("No labels detected");
            }
            
            // 5. 레이블명 추출
            String detectedText = labels.stream()
                .map(OcrResponse.DetectedLabel::getName)
                .collect(Collectors.joining(", "));
            System.out.println("Selected text: " + detectedText);

            // 5. Translate로 한글 번역 (빈 텍스트 처리)
            System.out.println("4. Translating text...");
            String translated;
            if (detectedText.isEmpty()) {
                translated = "감지된 객체가 없습니다.";
                System.out.println("No text to translate, using default message");
            } else {
                TranslateTextRequest trReq = TranslateTextRequest.builder()
                    .text(detectedText)
                    .sourceLanguageCode("en")
                    .targetLanguageCode("ko")
                    .build();
                translated = translateClient.translateText(trReq).translatedText();
                System.out.println("Translated text: " + translated);
            }

            // 6. Polly로 TTS (한글)
            System.out.println("5. Generating speech...");
            String audioBase64;
            if (translated.isEmpty() || translated.equals("감지된 객체가 없습니다.")) {
                audioBase64 = "";
                System.out.println("No text to synthesize, skipping audio generation");
            } else {
                SynthesizeSpeechRequest pollyReq = SynthesizeSpeechRequest.builder()
                    .text(translated)
                    .voiceId(VoiceId.SEOYEON)
                    .outputFormat(OutputFormat.MP3)
                    .engine(Engine.NEURAL)
                    .build();
                byte[] audioBytes = pollyClient.synthesizeSpeech(pollyReq).readAllBytes();
                audioBase64 = Base64.getEncoder().encodeToString(audioBytes);
                System.out.println("Audio generated, length: " + audioBytes.length);
            }

            // 7. 응답
            System.out.println("6. Creating response...");
            OcrResponse resp = new OcrResponse();
            resp.setDetectedLabels(labels);
            resp.setTranslatedText(translated);
            resp.setAudioBase64(audioBase64);
            resp.setSuccess(true);
            System.out.println("=== OCR SERVICE SUCCESS ===");
            return resp;
        } catch (Exception e) {
            System.err.println("=== OCR SERVICE ERROR ===");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Error Type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            System.err.println("========================");
            
            OcrResponse resp = new OcrResponse();
            resp.setSuccess(false);
            resp.setErrorMessage("오류: " + e.getMessage() + " (" + e.getClass().getSimpleName() + ")");
            return resp;
        }
    }
}
