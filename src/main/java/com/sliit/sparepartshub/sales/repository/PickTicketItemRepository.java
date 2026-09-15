package com.sliit.sparepartshub.sales.repository;

import com.sliit.sparepartshub.entity.PickTicketItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Scoped local copy for the same reason as PickTicketRepository in this
 * package - see that file's comment.
 */
public interface PickTicketItemRepository extends JpaRepository<PickTicketItem, Integer> {

    List<PickTicketItem> findByTicket_TicketId(Integer ticketId);
}
