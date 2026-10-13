package com.library.members.controller;

import com.library.common.dto.ApiResponse;
import com.library.members.model.Member;
import com.library.members.repository.MemberRepository;
import com.library.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Member>> getProfile(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Member member = memberRepository.findById(principal.getId()).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(member, "Profile details retrieved"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Member>>> getAllMembers() {
        List<Member> members = memberRepository.findAll();
        return ResponseEntity.ok(ApiResponse.ok(members, "All members retrieved"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Member>> getMemberById(@PathVariable String id) {
        Member member = memberRepository.findById(id).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(member, "Member retrieved"));
    }
}
