package com.sliit.sparepartshub.reporting.repository;

import com.sliit.sparepartshub.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    // Suppliers log into the portal with their email, same pattern as
    // staff login via UserRepository.findByEmail.
    Optional<Supplier> findByEmail(String email);
}
