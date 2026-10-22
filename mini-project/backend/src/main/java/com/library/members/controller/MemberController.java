package com.library.members.controller;

import com.library.common.dto.ApiResponse;
import com.library.members.dto.*;
import com.library.members.model.Member;
import com.library.members.model.Role;
import com.library.members.service.MemberService;
import com.library.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Member>> getProfile(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Member member = memberService.getMemberByIdOrEmail(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(member, "Profile details retrieved"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Member>>> getAllMembers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Role role) {
        List<Member> members = memberService.listMembers(search, status, role);
        return ResponseEntity.ok(ApiResponse.ok(members, "Members retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Member>> getMemberById(@PathVariable String id) {
        Member member = memberService.getMemberByIdOrEmail(id);
        return ResponseEntity.ok(ApiResponse.ok(member, "Member retrieved"));
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getMember360Profile(@PathVariable String id) {
        MemberProfileResponse profile = memberService.getMember360Profile(id);
        return ResponseEntity.ok(ApiResponse.ok(profile, "Member 360 profile retrieved"));
    }

    @GetMapping("/{id}/eligibility")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<MemberEligibilityResponse>> checkEligibility(@PathVariable String id) {
        MemberEligibilityResponse eligibility = memberService.checkEligibility(id);
        return ResponseEntity.ok(ApiResponse.ok(eligibility, "Member eligibility checked"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Member>> registerMember(
            @Valid @RequestBody CreateMemberRequest request,
            Authentication authentication) {
        String staffEmail = authentication != null ? authentication.getName() : "staff";
        Member registered = memberService.registerMemberByStaff(request, staffEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(registered, "Member registered successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Member>> updateMember(
            @PathVariable String id,
            @RequestBody UpdateMemberRequest request,
            Authentication authentication) {
        String staffEmail = authentication != null ? authentication.getName() : "staff";
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Member updated = memberService.updateMember(id, request, staffEmail, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Member updated successfully"));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Member>> changeMemberStatus(
            @PathVariable String id,
            @Valid @RequestBody MemberStatusUpdateRequest request,
            Authentication authentication) {
        String staffEmail = authentication != null ? authentication.getName() : "staff";
        Member updated = memberService.changeStatus(id, request, staffEmail);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Member status updated to " + updated.getStatus()));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable String id,
            @Valid @RequestBody ResetPasswordRequest request,
            Authentication authentication) {
        String staffEmail = authentication != null ? authentication.getName() : "staff";
        memberService.resetPassword(id, request.getNewPassword(), staffEmail);
        return ResponseEntity.ok(ApiResponse.ok(null, "Password reset successfully"));
    }
}
