package com.library.members.dto;

import com.library.circulation.model.Loan;
import com.library.fines.model.Fine;
import com.library.members.model.Member;
import com.library.reservations.model.Reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberProfileResponse {
    private Member member;
    private MemberEligibilityResponse eligibility;
    private List<Loan> activeLoans;
    private List<Loan> loanHistory;
    private List<Reservation> reservations;
    private List<Fine> unpaidFines;
    private double totalUnpaidFines;
}
