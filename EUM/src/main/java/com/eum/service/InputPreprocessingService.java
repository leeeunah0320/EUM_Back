package com.eum.service;

import com.eum.dto.ExtractedInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class InputPreprocessingService {

    private final BedrockService bedrockService;

    // 위치 관련 키워드 패턴
    private static final List<String> LOCATION_KEYWORDS = Arrays.asList(
        "역", "구", "동", "가", "로", "길", "대로", "시", "군", "구", "동", "리",
        "강남", "강북", "서초", "송파", "마포", "용산", "영등포", "금천", "관악", "동작",
        "서대문", "은평", "노원", "도봉", "강동", "광진", "성동", "중랑", "성북", "강북",
        "종로", "중구", "동대문", "중랑", "성북", "강북", "도봉", "노원", "은평", "서대문",
        "여의도", "홍대", "신촌", "이태원", "압구정", "청담", "삼성", "역삼", "선릉", "논현",
        "신사", "압구정", "청담", "삼성", "역삼", "선릉", "논현", "신사", "압구정", "청담"
    );

    // 음식/장소 관련 키워드
    private static final List<String> FOOD_KEYWORDS = Arrays.asList(
        "맛집", "식당", "레스토랑", "카페", "음식점", "한식", "중식", "일식", "양식", "분식",
        "치킨", "피자", "햄버거", "샐러드", "스시", "라멘", "돈까스", "파스타", "스테이크",
        "떡볶이", "김밥", "라면", "국수", "냉면", "비빔밥", "불고기", "갈비", "삼겹살",
        "회", "초밥", "우동", "라멘", "돈부리", "카레", "타코", "부리토", "샌드위치",
        "브런치", "디저트", "케이크", "아이스크림", "커피", "차", "주스", "음료"
    );

    // 장소 유형 키워드
    private static final List<String> PLACE_KEYWORDS = Arrays.asList(
        "추천", "찾아", "알려", "근처", "주변", "가까운", "좋은", "유명한", "인기",
        "맛있는", "저렴한", "비싼", "고급", "분위기", "데이트", "혼밥", "회식",
        "가족", "친구", "연인", "혼자", "여럿", "단체"
    );

    /**
     * 사용자 입력에서 위치와 키워드 추출
     */
    public ExtractedInfo extractLocationAndKeywords(String userQuery) {
        try {
            log.info("입력 전처리 시작: {}", userQuery);

            // 1. 위치 추출
            String location = extractLocation(userQuery);
            
            // 2. 키워드 추출
            List<String> keywords = extractKeywords(userQuery);
            
            // 3. Bedrock을 사용한 쿼리 전처리
            String processedQuery = bedrockService.preprocessQuery(userQuery);

            ExtractedInfo extractedInfo = ExtractedInfo.builder()
                    .location(location)
                    .keywords(keywords)
                    .originalQuery(userQuery)
                    .processedQuery(processedQuery)
                    .build();

            log.info("추출된 정보 - 위치: {}, 키워드: {}", location, keywords);
            return extractedInfo;

        } catch (Exception e) {
            log.error("입력 전처리 중 오류 발생", e);
            return ExtractedInfo.builder()
                    .location(null)
                    .keywords(new ArrayList<>())
                    .originalQuery(userQuery)
                    .processedQuery(userQuery)
                    .build();
        }
    }

    /**
     * 위치 정보 추출
     */
    private String extractLocation(String query) {
        String lowerQuery = query.toLowerCase();
        
        // 1. 주요 지명 패턴 매칭 (우선순위 높음)
        String[] majorLocations = {
            "강남역", "홍대", "신촌", "이태원", "압구정", "청담", "삼성역", "역삼역", 
            "선릉역", "논현역", "신사역", "강남구", "서초구", "송파구", "마포구", 
            "용산구", "영등포구", "여의도", "잠실", "건대", "성수", "왕십리"
        };
        
        for (String location : majorLocations) {
            if (lowerQuery.contains(location.toLowerCase())) {
                return location;
            }
        }
        
        // 2. 명시적인 위치 키워드 검색
        for (String locationKeyword : LOCATION_KEYWORDS) {
            if (lowerQuery.contains(locationKeyword)) {
                // 위치 키워드 주변의 텍스트 추출
                String location = extractLocationAroundKeyword(query, locationKeyword);
                if (location != null && !location.trim().isEmpty()) {
                    return location;
                }
            }
        }

        // 3. 정규표현식을 사용한 위치 패턴 매칭
        String location = extractLocationByPattern(query);
        if (location != null && !location.trim().isEmpty()) {
            return location;
        }

        return null;
    }

    /**
     * 키워드 주변의 위치 정보 추출
     */
    private String extractLocationAroundKeyword(String query, String keyword) {
        int keywordIndex = query.toLowerCase().indexOf(keyword);
        if (keywordIndex == -1) return null;

        // 키워드 앞뒤로 최대 10글자씩 추출
        int start = Math.max(0, keywordIndex - 10);
        int end = Math.min(query.length(), keywordIndex + keyword.length() + 10);
        
        String aroundKeyword = query.substring(start, end);
        
        // 한글, 영문, 숫자, 공백만 추출
        Pattern pattern = Pattern.compile("[가-힣a-zA-Z0-9\\s]+");
        Matcher matcher = pattern.matcher(aroundKeyword);
        
        if (matcher.find()) {
            return matcher.group().trim();
        }
        
        return null;
    }

    /**
     * 정규표현식을 사용한 위치 패턴 매칭
     */
    private String extractLocationByPattern(String query) {
        // "XX역", "XX구", "XX동" 등의 패턴
        Pattern locationPattern = Pattern.compile("([가-힣]+(?:역|구|동|가|로|길|대로))");
        Matcher matcher = locationPattern.matcher(query);
        
        if (matcher.find()) {
            return matcher.group(1);
        }

        // "XX시", "XX군" 등의 패턴
        locationPattern = Pattern.compile("([가-힣]+(?:시|군))");
        matcher = locationPattern.matcher(query);
        
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * 키워드 추출
     */
    private List<String> extractKeywords(String query) {
        List<String> keywords = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        // 1. 음식 관련 키워드 추출
        for (String foodKeyword : FOOD_KEYWORDS) {
            if (lowerQuery.contains(foodKeyword)) {
                keywords.add(foodKeyword);
            }
        }

        // 2. 장소 유형 키워드 추출
        for (String placeKeyword : PLACE_KEYWORDS) {
            if (lowerQuery.contains(placeKeyword)) {
                keywords.add(placeKeyword);
            }
        }

        // 3. 중복 제거 및 정렬
        keywords = new ArrayList<>(new LinkedHashSet<>(keywords));
        Collections.sort(keywords);

        return keywords;
    }

    /**
     * 추출된 정보를 기반으로 검색 쿼리 생성
     */
    public String generateSearchQuery(ExtractedInfo extractedInfo) {
        StringBuilder searchQuery = new StringBuilder();

        // 위치 정보 추가
        if (extractedInfo.getLocation() != null && !extractedInfo.getLocation().trim().isEmpty()) {
            searchQuery.append(extractedInfo.getLocation()).append(" ");
        }

        // 키워드 추가
        if (extractedInfo.getKeywords() != null && !extractedInfo.getKeywords().isEmpty()) {
            for (String keyword : extractedInfo.getKeywords()) {
                searchQuery.append(keyword).append(" ");
            }
        }

        // 기본 검색어 추가 (키워드가 없는 경우)
        if (searchQuery.toString().trim().isEmpty()) {
            searchQuery.append("맛집 추천");
        }

        return searchQuery.toString().trim();
    }
}
