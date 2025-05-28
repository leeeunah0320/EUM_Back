package com.eum.repository;

import com.eum.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
// 데이터베이스 접근 인터페이스
public interface MemberRepository extends JpaRepository<Member, Long> {
} 