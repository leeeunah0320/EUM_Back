package com.eum.service;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

public class RekognitionTrainService {
    public void trainModel() {
        RekognitionClient rekognitionClient = RekognitionClient.builder()
            .region(Region.US_EAST_1) // 서울 리전 예시
            .build();

        String projectArn = "arn:aws:rekognition:us-east-1:289979559306:project/EUM/1751557281118"; // 실제 프로젝트 ARN으로 교체

        CreateProjectVersionRequest trainRequest = CreateProjectVersionRequest.builder()
            .projectArn(projectArn)
            .versionName("v1")
            .outputConfig(OutputConfig.builder()
                .s3Bucket("custom-labels-console-us-east-1-3955b158b6") // 실제 버킷명으로 교체
                .s3KeyPrefix("output/") // (선택) 저장 경로
                .build())
            .build();

        CreateProjectVersionResponse trainResponse = rekognitionClient.createProjectVersion(trainRequest);
        String projectVersionArn = trainResponse.projectVersionArn();
        System.out.println("Training Job ARN: " + projectVersionArn);
    }

    public void checkTrainingStatus(String projectArn) {
        RekognitionClient rekognitionClient = RekognitionClient.builder()
            .region(Region.US_EAST_1) // 실제 리전
            .build();

        DescribeProjectVersionsRequest describeRequest = DescribeProjectVersionsRequest.builder()
            .projectArn(projectArn)
            .build();

        DescribeProjectVersionsResponse describeResponse = rekognitionClient.describeProjectVersions(describeRequest);
        describeResponse.projectVersionDescriptions().forEach(desc -> {
            System.out.println("Status: " + desc.status());
        });
    }
} 
