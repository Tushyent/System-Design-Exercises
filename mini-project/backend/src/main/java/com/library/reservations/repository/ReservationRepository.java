package com.library.reservations.repository;

import com.library.reservations.model.Reservation;
import com.library.reservations.model.ReservationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends MongoRepository<Reservation, String> {
    Optional<Reservation> findByReservationId(String reservationId);
    Optional<Reservation> findFirstByBookIdAndStatusOrderByQueuePositionAsc(String bookId, ReservationStatus status);
    List<Reservation> findByMemberIdOrderByCreatedAtDesc(String memberId);
    Optional<Reservation> findByBookIdAndMemberIdAndStatus(String bookId, String memberId, ReservationStatus status);
    int countByBookIdAndStatus(String bookId, ReservationStatus status);
}
