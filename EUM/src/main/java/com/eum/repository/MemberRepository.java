package com.eum.repository;

import com.eum.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
// 데이터베이스 접근 인터페이스
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsernameAndNameAndEmail(String username, String name, String email);
    Optional<Member> findByUsername(String username);
} 
