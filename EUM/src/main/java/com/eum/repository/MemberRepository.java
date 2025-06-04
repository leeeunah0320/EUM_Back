package com.eum.repository;

import com.eum.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
// 데이터베이스 접근 인터페이스
import java.util.Optional;
import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsernameAndNameAndEmail(String username, String name, String email);
    Optional<Member> findByUsername(String username);
    Optional<Member> findByNameAndEmail(String name, String email);
    Optional<Member> findByEmail(String email);
    List<Member> findAllByNameAndEmail(String name, String email);
    List<Member> findAllByEmail(String email);
} 
