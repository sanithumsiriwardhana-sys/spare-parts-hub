package com.sliit.sparepartshub.sales.repository;

import com.sliit.sparepartshub.entity.SaleItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface SalesSaleItemRepository extends JpaRepository<SaleItem,Integer>{ @EntityGraph(attributePaths="product") List<SaleItem> findBySale_SaleId(Integer saleId); }
