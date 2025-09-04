package com.eum.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExtractedInfo {
    private String location; // 추출된 위치 (예: 강남역)
    private List<String> keywords; // 추출된 키워드들 (예: 중식집, 맛집)
    private String originalQuery; // 원본 쿼리
    private String processedQuery; // 전처리된 쿼리
}
