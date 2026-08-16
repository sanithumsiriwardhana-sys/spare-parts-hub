package com.sliit.sparepartshub.stockmonitoring.repository;

import com.sliit.sparepartshub.entity.RestockSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestockSuggestionRepository extends JpaRepository<RestockSuggestion, Integer> {

    // Supervisor's review queue (UC-03 steps 7-9).
    List<RestockSuggestion> findByStatus(RestockSuggestion.Status status);

    List<RestockSuggestion> findByProduct_ProductIdOrderByCreatedAtDesc(Integer productId);
}
