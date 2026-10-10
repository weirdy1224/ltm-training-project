package com.examly.springapp.repository;

import com.examly.springapp.model.Ticket;
import com.examly.springapp.model.TicketStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepo extends JpaRepository<Ticket, Long> {
    List<Ticket> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Ticket> findAllByOrderByCreatedAtDesc();

    boolean existsByTicketCode(String ticketCode);

    /** True when the ticket with this id is in one of the given states (used to lock admin notifications). */
    boolean existsByIdAndStatusIn(Long id, Collection<TicketStatus> statuses);
}