package com.eum.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

@Service
public class RekognitionDetectService {
    public String detectCustomLabel() {
        RekognitionClient rekognitionClient = RekognitionClient.builder()
            .region(Region.US_EAST_1)
            .build();

        DetectCustomLabelsRequest request = DetectCustomLabelsRequest.builder()
            .projectVersionArn("arn:aws:rekognition:us-east-1:289979559306:project/EUM/version/EUM.2025-07-04T18.35.53/1751621754095")
            .image(Image.builder()
                .s3Object(S3Object.builder()
                    .bucket("custom-labels-console-us-east-1-3955b158b6")
                    .name("black bean noodles/black bean noodles1.jpg")
                    .build())
                .build())
            .build();

        DetectCustomLabelsResponse response = rekognitionClient.detectCustomLabels(request);

        StringBuilder result = new StringBuilder();
        for (CustomLabel label : response.customLabels()) {
            result.append("Label: ").append(label.name())
                  .append(", Confidence: ").append(label.confidence()).append("\\n");
        }
        return result.toString();
    }
}
