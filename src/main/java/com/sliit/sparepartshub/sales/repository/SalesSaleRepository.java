package com.sliit.sparepartshub.sales.repository;

import com.sliit.sparepartshub.entity.Sale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
public interface SalesSaleRepository extends JpaRepository<Sale,Integer>{ @Override @EntityGraph(attributePaths="soldBy") Optional<Sale> findById(Integer saleId); }
