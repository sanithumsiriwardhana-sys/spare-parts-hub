package com.sliit.sparepartshub.warranty.repository;

import com.sliit.sparepartshub.entity.SerialNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for physical item serial numbers (Function 4: Warranty and Returns Management).
 * Primary owner of the SerialNumber entity.
 */
@Repository
public interface SerialNumberRepository extends JpaRepository<SerialNumber, Integer> {

    Optional<SerialNumber> findBySerialValue(String serialValue);

    Optional<SerialNumber> findBySerialValueIgnoreCase(String serialValue);

    List<SerialNumber> findByCurrentStatus(SerialNumber.CurrentStatus currentStatus);

    List<SerialNumber> findByProduct_ProductId(Integer productId);

    List<SerialNumber> findByProduct_ProductIdAndCurrentStatus(Integer productId, SerialNumber.CurrentStatus currentStatus);

    long countByCurrentStatus(SerialNumber.CurrentStatus currentStatus);

    boolean existsBySerialValue(String serialValue);
}
