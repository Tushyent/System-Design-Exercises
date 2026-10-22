package com.library.members.service;

import com.library.audit.service.AuditLogService;
import com.library.circulation.model.Loan;
import com.library.circulation.model.LoanStatus;
import com.library.circulation.repository.LoanRepository;
import com.library.common.exception.ConflictException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.fines.model.Fine;
import com.library.fines.model.FineStatus;
import com.library.fines.repository.FineRepository;
import com.library.idgenerator.SnowflakeIdGenerator;
import com.library.members.dto.*;
import com.library.members.model.Member;
import com.library.members.model.Role;
import com.library.members.repository.MemberRepository;
import com.library.reservations.model.Reservation;
import com.library.reservations.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;
    private final ReservationRepository reservationRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public List<Member> listMembers(String query, String status, Role role) {
        List<Member> list;
        if (query != null && !query.isBlank()) {
            String q = query.trim();
            list = memberRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrMemberIdContainingIgnoreCase(q, q, q);
        } else if (role != null) {
            list = memberRepository.findByRole(role);
        } else if (status != null && !status.isBlank()) {
            list = memberRepository.findByStatus(status.toUpperCase());
        } else {
            list = memberRepository.findAll();
        }

        // Apply secondary filters in-memory if needed
        return list.stream()
                .filter(m -> role == null || m.getRole() == role)
                .filter(m -> status == null || status.isBlank() || status.equalsIgnoreCase(m.getStatus()))
                .collect(Collectors.toList());
    }

    public Member getMemberByIdOrEmail(String identifier) {
        return memberRepository.findById(identifier)
                .or(() -> memberRepository.findByEmail(identifier))
                .or(() -> memberRepository.findByMemberId(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("Member not found for identifier: " + identifier));
    }

    public MemberEligibilityResponse checkEligibility(String memberId) {
        Member member = getMemberByIdOrEmail(memberId);
        int activeLoans = loanRepository.countByMemberIdAndStatus(member.getId(), LoanStatus.ACTIVE);
        List<Fine> unpaidFines = fineRepository.findByMemberIdAndStatus(member.getId(), FineStatus.UNPAID);
        double totalUnpaid = unpaidFines.stream().mapToDouble(Fine::getAmount).sum();

        boolean eligible = true;
        StringBuilder reason = new StringBuilder();

        if (!"ACTIVE".equalsIgnoreCase(member.getStatus())) {
            eligible = false;
            reason.append("Account is ").append(member.getStatus());
            if (member.getSuspensionReason() != null) {
                reason.append(" (").append(member.getSuspensionReason()).append(")");
            }
            reason.append(". ");
        }

        if (activeLoans >= member.getMaxBorrowLimit()) {
            eligible = false;
            reason.append("Borrow limit reached (").append(activeLoans).append("/").append(member.getMaxBorrowLimit()).append("). ");
        }

        if (totalUnpaid > 20.00) {
            eligible = false;
            reason.append(String.format("Unpaid fines of INR %.2f exceed threshold of INR 20.00. ", totalUnpaid));
        }

        return MemberEligibilityResponse.builder()
                .memberId(member.getMemberId())
                .memberName(member.getName())
                .status(member.getStatus())
                .eligible(eligible)
                .activeLoans(activeLoans)
                .maxBorrowLimit(member.getMaxBorrowLimit())
                .unpaidFinesAmount(totalUnpaid)
                .ineligibilityReason(eligible ? null : reason.toString().trim())
                .build();
    }

    public MemberProfileResponse getMember360Profile(String memberId) {
        Member member = getMemberByIdOrEmail(memberId);
        MemberEligibilityResponse eligibility = checkEligibility(member.getId());

        List<Loan> activeLoans = loanRepository.findByMemberIdAndStatus(member.getId(), LoanStatus.ACTIVE);
        List<Loan> history = loanRepository.findByMemberIdOrderByBorrowedAtDesc(member.getId());
        List<Reservation> reservations = reservationRepository.findByMemberIdOrderByCreatedAtDesc(member.getId());
        List<Fine> unpaidFines = fineRepository.findByMemberIdAndStatus(member.getId(), FineStatus.UNPAID);
        double totalFines = unpaidFines.stream().mapToDouble(Fine::getAmount).sum();

        return MemberProfileResponse.builder()
                .member(member)
                .eligibility(eligibility)
                .activeLoans(activeLoans)
                .loanHistory(history)
                .reservations(reservations)
                .unpaidFines(unpaidFines)
                .totalUnpaidFines(totalFines)
                .build();
    }

    public Member registerMemberByStaff(CreateMemberRequest request, String staffEmail) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        String rawId = idGenerator.nextIdString();
        String memberCode = "MEM-" + rawId.substring(Math.max(0, rawId.length() - 5));

        Member member = Member.builder()
                .id(rawId)
                .memberId(memberCode)
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.MEMBER)
                .status("ACTIVE")
                .department(request.getDepartment())
                .phone(request.getPhone())
                .maxBorrowLimit(request.getMaxBorrowLimit() > 0 ? request.getMaxBorrowLimit() : 5)
                .createdAt(Instant.now())
                .build();

        Member saved = memberRepository.save(member);
        auditLogService.record(staffEmail, "STAFF", "MEMBER_REGISTER", "MEMBER", saved.getMemberId(),
                "Registered new member: " + saved.getName() + " (" + saved.getEmail() + ")");
        return saved;
    }

    public Member updateMember(String memberId, UpdateMemberRequest request, String staffEmail, boolean isAdmin) {
        Member member = getMemberByIdOrEmail(memberId);

        if (request.getName() != null && !request.getName().isBlank()) {
            member.setName(request.getName());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank() && !request.getEmail().equalsIgnoreCase(member.getEmail())) {
            if (memberRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("Email already in use: " + request.getEmail());
            }
            member.setEmail(request.getEmail());
        }
        if (request.getDepartment() != null) {
            member.setDepartment(request.getDepartment());
        }
        if (request.getPhone() != null) {
            member.setPhone(request.getPhone());
        }
        if (request.getMaxBorrowLimit() != null && request.getMaxBorrowLimit() > 0) {
            member.setMaxBorrowLimit(request.getMaxBorrowLimit());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            member.setStatus(request.getStatus().toUpperCase());
        }
        if (isAdmin && request.getRole() != null) {
            member.setRole(request.getRole());
        }

        Member updated = memberRepository.save(member);
        auditLogService.record(staffEmail, "STAFF", "MEMBER_UPDATE", "MEMBER", updated.getMemberId(),
                "Updated profile for: " + updated.getEmail());
        return updated;
    }

    public Member changeStatus(String memberId, MemberStatusUpdateRequest request, String staffEmail) {
        Member member = getMemberByIdOrEmail(memberId);
        String oldStatus = member.getStatus();
        member.setStatus(request.getStatus().toUpperCase());

        if ("SUSPENDED".equalsIgnoreCase(request.getStatus())) {
            member.setSuspensionReason(request.getReason());
            member.setSuspendedUntil(request.getSuspendedUntil());
        } else if ("ACTIVE".equalsIgnoreCase(request.getStatus())) {
            member.setSuspensionReason(null);
            member.setSuspendedUntil(null);
        }

        Member saved = memberRepository.save(member);
        auditLogService.record(staffEmail, "STAFF", "MEMBER_STATUS_CHANGE", "MEMBER", saved.getMemberId(),
                String.format("Status changed %s -> %s. Reason: %s", oldStatus, saved.getStatus(), request.getReason()));
        return saved;
    }

    public void resetPassword(String memberId, String newPassword, String staffEmail) {
        Member member = getMemberByIdOrEmail(memberId);
        member.setPasswordHash(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
        auditLogService.record(staffEmail, "STAFF", "PASSWORD_RESET", "MEMBER", member.getMemberId(),
                "Password reset for: " + member.getEmail());
    }
}
