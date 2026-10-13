package com.library.members.repository;

import com.library.members.model.Member;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends MongoRepository<Member, String> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByMemberId(String memberId);
    boolean existsByEmail(String email);
}
