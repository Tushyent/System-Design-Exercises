package com.library.reservations.service;

import com.library.catalogue.model.Book;
import com.library.catalogue.model.BookCopy;
import com.library.catalogue.model.CopyStatus;
import com.library.catalogue.repository.BookCopyRepository;
import com.library.catalogue.repository.BookRepository;
import com.library.common.exception.ConflictException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.idgenerator.SnowflakeIdGenerator;
import com.library.members.model.Member;
import com.library.members.repository.MemberRepository;
import com.library.reservations.dto.CreateReservationRequest;
import com.library.reservations.model.Reservation;
import com.library.reservations.model.ReservationStatus;
import com.library.reservations.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final MemberRepository memberRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final com.library.audit.service.AuditLogService auditLogService;

    public Reservation createReservation(CreateReservationRequest request, String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + request.getBookId()));

        // Prevent duplicate active reservation for same book by same member
        if (reservationRepository.findByBookIdAndMemberIdAndStatus(book.getId(), memberId, ReservationStatus.PENDING).isPresent()
                || reservationRepository.findByBookIdAndMemberIdAndStatus(book.getId(), memberId, ReservationStatus.READY_FOR_PICKUP).isPresent()) {
            throw new ConflictException("You already have an active reservation for '" + book.getTitle() + "'");
        }

        int currentQueueSize = reservationRepository.countByBookIdAndStatus(book.getId(), ReservationStatus.PENDING);
        int queuePosition = currentQueueSize + 1;

        String reservationId = idGenerator.nextPrefixedId("RES");
        Reservation reservation = Reservation.builder()
                .id(idGenerator.nextIdString())
                .reservationId(reservationId)
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .memberId(member.getId())
                .memberEmail(member.getEmail())
                .queuePosition(queuePosition)
                .status(ReservationStatus.PENDING)
                .build();

        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation {} created for member {} for book '{}' at queue position {}",
                reservationId, memberId, book.getTitle(), queuePosition);
        return saved;
    }

    public List<Reservation> getMemberReservations(String memberId) {
        return reservationRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    public Reservation cancelReservation(String reservationId, String memberId) {
        Reservation reservation = reservationRepository.findByReservationId(reservationId)
                .or(() -> reservationRepository.findById(reservationId))
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        if (!reservation.getMemberId().equals(memberId)) {
            throw new ConflictException("Cannot cancel another member's reservation");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.READY_FOR_PICKUP) {
            throw new ConflictException("Reservation is already " + reservation.getStatus());
        }

        // If it was allocated a copy, free the copy or pass to next in queue
        if (reservation.getAllocatedCopyId() != null) {
            BookCopy copy = copyRepository.findById(reservation.getAllocatedCopyId()).orElse(null);
            if (copy != null) {
                // Check if next reservation waiting
                Reservation nextRes = reservationRepository
                        .findFirstByBookIdAndStatusOrderByQueuePositionAsc(copy.getBookId(), ReservationStatus.PENDING)
                        .orElse(null);

                if (nextRes != null) {
                    nextRes.setStatus(ReservationStatus.READY_FOR_PICKUP);
                    nextRes.setAllocatedCopyId(copy.getId());
                    reservationRepository.save(nextRes);
                    copy.setCurrentHolderId(nextRes.getMemberId());
                } else {
                    copy.setStatus(CopyStatus.AVAILABLE);
                    copy.setCurrentHolderId(null);
                    copy.setPickupDeadline(null);
                }
                copyRepository.save(copy);
            }
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservationRepository.save(reservation);
    }

    public List<Reservation> getAllReservations(ReservationStatus status) {
        if (status != null) {
            return reservationRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return reservationRepository.findAllByOrderByCreatedAtDesc();
    }

    public Reservation fulfillReservation(String reservationId, String staffEmail) {
        Reservation reservation = reservationRepository.findByReservationId(reservationId)
                .or(() -> reservationRepository.findById(reservationId))
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        reservation.setStatus(ReservationStatus.FULFILLED);
        Reservation saved = reservationRepository.save(reservation);

        auditLogService.record(staffEmail, "STAFF", "RESERVATION_FULFILL", "RESERVATION", reservation.getReservationId(),
                "Marked reservation as fulfilled for member: " + reservation.getMemberEmail());
        return saved;
    }

    public Reservation cancelReservationByStaff(String reservationId, String reason, String staffEmail) {
        Reservation reservation = reservationRepository.findByReservationId(reservationId)
                .or(() -> reservationRepository.findById(reservationId))
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        if (reservation.getAllocatedCopyId() != null) {
            BookCopy copy = copyRepository.findById(reservation.getAllocatedCopyId()).orElse(null);
            if (copy != null) {
                copy.setStatus(CopyStatus.AVAILABLE);
                copy.setCurrentHolderId(null);
                copy.setPickupDeadline(null);
                copyRepository.save(copy);
            }
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepository.save(reservation);

        auditLogService.record(staffEmail, "STAFF", "RESERVATION_CANCEL", "RESERVATION", reservation.getReservationId(),
                "Staff cancelled reservation. Reason: " + reason);
        return saved;
    }

    public Reservation expireReservation(String reservationId, String staffEmail) {
        Reservation reservation = reservationRepository.findByReservationId(reservationId)
                .or(() -> reservationRepository.findById(reservationId))
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        if (reservation.getAllocatedCopyId() != null) {
            BookCopy copy = copyRepository.findById(reservation.getAllocatedCopyId()).orElse(null);
            if (copy != null) {
                copy.setStatus(CopyStatus.AVAILABLE);
                copy.setCurrentHolderId(null);
                copy.setPickupDeadline(null);
                copyRepository.save(copy);
            }
        }

        reservation.setStatus(ReservationStatus.EXPIRED);
        Reservation saved = reservationRepository.save(reservation);

        auditLogService.record(staffEmail, "STAFF", "RESERVATION_EXPIRE", "RESERVATION", reservation.getReservationId(),
                "Marked unclaimed hold as expired for: " + reservation.getMemberEmail());
        return saved;
    }
}
