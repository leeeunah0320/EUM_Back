package com.eum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.*;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class PollyService {

    private final PollyClient pollyClient;

    /**
     * 텍스트를 음성으로 변환하고 Base64로 인코딩
     */
    public String convertTextToSpeech(String text) {
        try {
            log.info("Polly TTS 변환 시작: text length={}", text.length());

            // Polly 요청 생성
            SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
                .text(text)
                .voiceId(VoiceId.SEOYEON) // 한국어 음성
                .outputFormat(OutputFormat.MP3)
                .engine(Engine.NEURAL)
                .build();

            // 음성 합성 실행
            ResponseInputStream<SynthesizeSpeechResponse> response = pollyClient.synthesizeSpeech(request);

            // 오디오 데이터를 바이트 배열로 변환
            ByteArrayOutputStream audioStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = response.read(buffer)) != -1) {
                audioStream.write(buffer, 0, bytesRead);
            }
            byte[] audioData = audioStream.toByteArray();

            // Base64로 인코딩
            String base64Audio = Base64.getEncoder().encodeToString(audioData);

            log.info("Polly TTS 변환 완료: audio size={} bytes", audioData.length);
            return base64Audio;

        } catch (Exception e) {
            log.error("Polly TTS 변환 중 오류 발생", e);
            return null;
        }
    }


    /**
     * 지원되는 음성 목록 조회
     */
    public String[] getAvailableVoices() {
        try {
            DescribeVoicesRequest request = DescribeVoicesRequest.builder()
                .languageCode(LanguageCode.KO_KR)
                .build();

            DescribeVoicesResponse response = pollyClient.describeVoices(request);
            
            return response.voices().stream()
                .map(Voice::id)
                .map(VoiceId::toString)
                .toArray(String[]::new);

        } catch (Exception e) {
            log.error("음성 목록 조회 중 오류 발생", e);
            return new String[]{VoiceId.SEOYEON.toString()};
        }
    }

    /**
     * 서비스 상태 확인
     */
    public boolean isServiceAvailable() {
        try {
            DescribeVoicesRequest request = DescribeVoicesRequest.builder()
                .languageCode(LanguageCode.KO_KR)
                .build();

            pollyClient.describeVoices(request);
            return true;

        } catch (Exception e) {
            log.error("Polly 서비스 상태 확인 중 오류 발생", e);
            return false;
        }
    }
}
