package com.sliit.sparepartshub.supplier.repository;

import com.sliit.sparepartshub.entity.PartnershipRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PartnershipRequestRepository extends JpaRepository<PartnershipRequest, Integer> {
    List<PartnershipRequest> findAllByOrderBySubmittedAtDesc();
    List<PartnershipRequest> findByStatusOrderBySubmittedAtDesc(PartnershipRequest.Status status);
}
