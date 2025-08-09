package com.eum.service;

import com.eum.dto.TtsRequest;
import com.eum.dto.TtsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class TtsService {

    private final PollyClient pollyClient;

    @Value("${aws.region}")
    private String region;

    /**
     * 텍스트를 음성으로 변환
     */
    public TtsResponse convertTextToSpeech(TtsRequest request) {
        try {
            log.info("TTS 변환 시작: {}", request.getText().substring(0, Math.min(50, request.getText().length())));
            log.info("TTS 요청 데이터 - voiceId: '{}', outputFormat: '{}', engine: '{}', languageCode: '{}'", 
                    request.getVoiceId(), request.getOutputFormat(), request.getEngine(), request.getLanguageCode());

            // 기본값 설정 - 더 안전한 방식
            String voiceId = (request.getVoiceId() != null && !request.getVoiceId().trim().isEmpty()) ? 
                           request.getVoiceId() : "Seoyeon";
            String outputFormat = (request.getOutputFormat() != null && !request.getOutputFormat().trim().isEmpty()) ? 
                                request.getOutputFormat() : "mp3";
            String engine = (request.getEngine() != null && !request.getEngine().trim().isEmpty()) ? 
                          request.getEngine() : "neural";
            String languageCode = (request.getLanguageCode() != null && !request.getLanguageCode().trim().isEmpty()) ? 
                                request.getLanguageCode() : "ko-KR";

            log.info("TTS 설정 - voiceId: {}, outputFormat: {}, engine: {}, languageCode: {}", 
                    voiceId, outputFormat, engine, languageCode);

            // null 체크 추가
            if (outputFormat == null) {
                log.error("outputFormat이 null입니다!");
                outputFormat = "mp3";
            }
            if (voiceId == null) {
                log.error("voiceId가 null입니다!");
                voiceId = "Seoyeon";
            }
            if (engine == null) {
                log.error("engine이 null입니다!");
                engine = "neural";
            }
            if (languageCode == null) {
                log.error("languageCode가 null입니다!");
                languageCode = "ko-KR";
            }

            // SynthesizeSpeechRequest 생성
            log.info("AWS Polly 요청 생성 - text: {}, voiceId: {}, outputFormat: {}, engine: {}, languageCode: {}", 
                    request.getText().substring(0, Math.min(50, request.getText().length())), 
                    voiceId, outputFormat, engine, languageCode);
            
            // 직접 enum 값 사용
            SynthesizeSpeechRequest.Builder requestBuilder = SynthesizeSpeechRequest.builder()
                    .text(request.getText())
                    .voiceId(VoiceId.SEOYEON)
                    .outputFormat(OutputFormat.MP3)
                    .engine(Engine.NEURAL)
                    .languageCode(LanguageCode.KO_KR);

            // API 호출
            ResponseInputStream<SynthesizeSpeechResponse> response = pollyClient.synthesizeSpeech(requestBuilder.build());

            // 오디오 데이터 읽기
            ByteArrayOutputStream audioStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = response.read(buffer)) != -1) {
                audioStream.write(buffer, 0, bytesRead);
            }

            byte[] audioData = audioStream.toByteArray();
            String audioBase64 = Base64.getEncoder().encodeToString(audioData);

            // 응답 생성
            TtsResponse ttsResponse = new TtsResponse();
            ttsResponse.setAudioBase64(audioBase64);
            ttsResponse.setText(request.getText());
            ttsResponse.setVoiceId(voiceId);
            ttsResponse.setLanguageCode(request.getLanguageCode() != null ? request.getLanguageCode() : "ko-KR");
            ttsResponse.setFormat(outputFormat);
            ttsResponse.setDuration(estimateDuration(request.getText()));

            log.info("TTS 변환 완료: {} 바이트", audioData.length);
            return ttsResponse;

        } catch (Exception e) {
            log.error("TTS 변환 중 오류 발생", e);
            throw new RuntimeException("음성 변환 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용 가능한 음성 목록 조회
     */
    public String getAvailableVoices() {
        try {
            DescribeVoicesRequest request = DescribeVoicesRequest.builder()
                    .languageCode(LanguageCode.KO_KR)
                    .engine(Engine.NEURAL)
                    .build();

            DescribeVoicesResponse response = pollyClient.describeVoices(request);

            StringBuilder voices = new StringBuilder();
            voices.append("사용 가능한 한국어 음성:\n\n");

            for (Voice voice : response.voices()) {
                voices.append("• ").append(voice.name()).append(" (").append(voice.gender().toString()).append(")\n");
            }

            return voices.toString();

        } catch (Exception e) {
            log.error("음성 목록 조회 중 오류 발생", e);
            return "음성 목록을 조회할 수 없습니다.";
        }
    }

    /**
     * 텍스트 길이에 따른 대략적인 재생 시간 추정 (초 단위)
     */
    private Long estimateDuration(String text) {
        // 한국어 기준으로 대략적으로 추정 (1초에 약 3-4음절)
        int syllableCount = text.length(); // 간단한 추정
        return Math.max(1L, syllableCount / 3L);
    }

    /**
     * 장소 정보를 음성으로 변환
     */
    public TtsResponse convertPlaceInfoToSpeech(String placeInfo) {
        TtsRequest request = new TtsRequest();
        request.setText(placeInfo);
        request.setVoiceId("Seoyeon");
        request.setLanguageCode("ko-KR");
        request.setOutputFormat("mp3");
        request.setEngine("neural");

        return convertTextToSpeech(request);
    }

    /**
     * 여러 장소 정보를 음성으로 변환
     */
    public TtsResponse convertMultiplePlacesToSpeech(String placesInfo) {
        TtsRequest request = new TtsRequest();
        request.setText(placesInfo);
        request.setVoiceId("Seoyeon");
        request.setLanguageCode("ko-KR");
        request.setOutputFormat("mp3");
        request.setEngine("neural");

        return convertTextToSpeech(request);
    }
}
