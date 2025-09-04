package com.eum.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class SpeechToTextService {

    @Value("${ai.speech-to-text.google-cloud.language-code}")
    private String languageCode;

    @Value("${ai.speech-to-text.google-cloud.encoding}")
    private String encoding;

    @Value("${ai.speech-to-text.google-cloud.sample-rate-hertz}")
    private int sampleRateHertz;

    @Value("${ai.speech-to-text.google-cloud.credentials-file}")
    private String credentialsFile;

    /**
     * Base64 인코딩된 오디오 데이터를 텍스트로 변환
     */
    public String convertAudioToText(String base64AudioData) {
        try {
            log.info("STT 변환 시작");
            
            // Base64 디코딩
            byte[] audioBytes = Base64.getDecoder().decode(base64AudioData);
            
            // Google Cloud 인증 설정
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsFile));
            
            // SpeechClient 생성 (인증 정보 포함)
            try (SpeechClient speechClient = SpeechClient.create(SpeechSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build())) {
                
                // 오디오 설정
                RecognitionConfig config = RecognitionConfig.newBuilder()
                        .setLanguageCode(languageCode)
                        .setEncoding(RecognitionConfig.AudioEncoding.valueOf(encoding))
                        .setSampleRateHertz(sampleRateHertz)
                        .build();

                // 오디오 데이터 설정
                RecognitionAudio audio = RecognitionAudio.newBuilder()
                        .setContent(ByteString.copyFrom(audioBytes))
                        .build();

                // STT 요청
                RecognizeResponse response = speechClient.recognize(config, audio);
                List<SpeechRecognitionResult> results = response.getResultsList();

                StringBuilder transcript = new StringBuilder();
                for (SpeechRecognitionResult result : results) {
                    List<SpeechRecognitionAlternative> alternatives = result.getAlternativesList();
                    for (SpeechRecognitionAlternative alternative : alternatives) {
                        transcript.append(alternative.getTranscript());
                    }
                }

                String result = transcript.toString();
                log.info("STT 변환 결과: {}", result);
                return result;

            } catch (IOException e) {
                log.error("SpeechClient 생성 중 오류 발생", e);
                throw new RuntimeException("STT 서비스 초기화 실패", e);
            }

        } catch (Exception e) {
            log.error("오디오를 텍스트로 변환하는 중 오류 발생", e);
            
            // 구체적인 에러 메시지 제공
            if (e.getMessage() != null && e.getMessage().contains("billing")) {
                log.warn("Google Cloud Speech-to-Text API 결제가 활성화되지 않았습니다.");
                return "음성 인식 서비스를 사용하려면 Google Cloud 결제 설정이 필요합니다. 현재는 텍스트 입력을 사용해주세요.";
            } else if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                log.warn("Google Cloud Speech-to-Text API 권한이 없습니다.");
                return "음성 인식 서비스 권한이 설정되지 않았습니다. 현재는 텍스트 입력을 사용해주세요.";
            } else {
                log.warn("STT 서비스가 사용할 수 없습니다. 테스트 모드로 작동합니다.");
                return "음성 인식 서비스에 일시적인 문제가 발생했습니다. 텍스트로 입력해주세요.";
            }
        }
    }

    /**
     * 오디오 데이터 유효성 검사
     */
    public boolean isValidAudioData(String base64AudioData) {
        try {
            if (base64AudioData == null || base64AudioData.trim().isEmpty()) {
                return false;
            }
            
            // Base64 디코딩 테스트
            Base64.getDecoder().decode(base64AudioData);
            return true;
            
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 Base64 인코딩: {}", e.getMessage());
            return false;
        }
    }
}
