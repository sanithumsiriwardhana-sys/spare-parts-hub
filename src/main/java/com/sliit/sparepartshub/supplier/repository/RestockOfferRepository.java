package com.sliit.sparepartshub.supplier.repository;

import com.sliit.sparepartshub.entity.RestockOffer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RestockOfferRepository extends JpaRepository<RestockOffer, Integer> {
    @EntityGraph(attributePaths = {"supplier", "product"})
    List<RestockOffer> findByProduct_ProductIdOrderBySubmittedAtDesc(Integer productId);
}
