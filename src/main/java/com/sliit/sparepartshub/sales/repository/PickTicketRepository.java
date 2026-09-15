package com.sliit.sparepartshub.sales.repository;

import com.sliit.sparepartshub.entity.PickTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * PickTicket belongs conceptually to Function 1 (inventory), owned by
 * Rathnayake per Use_Case_Scenarios.docx - but that package doesn't
 * exist yet, and UC-02 step 14 requires checkout to generate a pick
 * ticket the moment a sale completes. This is a deliberate, scoped
 * local copy, following the exact pattern stockmonitoring used for
 * Product before Function 2 existed (see stockmonitoring/repository/
 * ProductRepository.java's comment). Consolidate into inventory's own
 * repository once that package is built - flagged to the team per
 * CONTRIBUTING.md, don't silently duplicate an owned repository.
 */
public interface PickTicketRepository extends JpaRepository<PickTicket, Integer> {

    Optional<PickTicket> findBySale_SaleId(Integer saleId);
}
