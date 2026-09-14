package com.sliit.sparepartshub.warranty.repository;

import com.sliit.sparepartshub.entity.RmaClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RMA Claims (Function 4: Warranty and Returns Management).
 * Primary owner of the RmaClaim entity.
 */
@Repository
public interface RmaClaimRepository extends JpaRepository<RmaClaim, Integer> {

    List<RmaClaim> findAllByOrderByClaimDateDescClaimIdDesc();

    List<RmaClaim> findByResolutionOrderByClaimDateDescClaimIdDesc(RmaClaim.Resolution resolution);

    List<RmaClaim> findBySerial_SerialIdOrderByClaimDateDesc(Integer serialId);

    List<RmaClaim> findBySerial_SerialValueOrderByClaimDateDesc(String serialValue);

    Optional<RmaClaim> findByClaimCode(String claimCode);

    boolean existsByClaimCode(String claimCode);

    long countByResolution(RmaClaim.Resolution resolution);

    @Query("SELECT c FROM RmaClaim c " +
           "WHERE (:resolution IS NULL OR c.resolution = :resolution) " +
           "AND (:search IS NULL OR LOWER(c.claimCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(c.serial.serialValue) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(c.serial.product.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.claimDate DESC, c.claimId DESC")
    List<RmaClaim> searchClaims(@Param("resolution") RmaClaim.Resolution resolution,
                                @Param("search") String search);
}
