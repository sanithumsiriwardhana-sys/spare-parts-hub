package com.sliit.sparepartshub.stockmonitoring.repository;

import com.sliit.sparepartshub.entity.StockRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockRequestRepository extends JpaRepository<StockRequest, Integer> {

    // Supervisor's "notify customer when stock arrives" queue (PBI-11).
    List<StockRequest> findByStatus(StockRequest.Status status);

    List<StockRequest> findByProduct_ProductIdOrderByRequestedAtDesc(Integer productId);
}
