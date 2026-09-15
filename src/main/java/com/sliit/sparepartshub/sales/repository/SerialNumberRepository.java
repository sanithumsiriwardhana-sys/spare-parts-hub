package com.sliit.sparepartshub.sales.repository;

import com.sliit.sparepartshub.entity.SerialNumber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * serial_number doesn't have a clear single owner yet: Function 5
 * (Supplier Management) will create rows on receipt, Function 4
 * (Warranty/RMA) will read them to validate claims - sales only needs
 * to read in_stock rows for the picker and perform the specific
 * in_stock -> sold transition at checkout (UC-02 postcondition 3). This
 * is a scoped, limited-purpose repository, not a general-purpose one -
 * same reasoning as PickTicketRepository in this package. Flag to the
 * team once Function 4/5 exist so nobody else duplicates it blind.
 */
public interface SerialNumberRepository extends JpaRepository<SerialNumber, Integer> {

    List<SerialNumber> findByProduct_ProductIdAndCurrentStatus(Integer productId, SerialNumber.CurrentStatus status);

    // Used to build the receipt - which specific units went out on this sale.
    List<SerialNumber> findBySale_SaleId(Integer saleId);
}
