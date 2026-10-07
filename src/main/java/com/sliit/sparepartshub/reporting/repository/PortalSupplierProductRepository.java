package com.sliit.sparepartshub.reporting.repository;

import com.sliit.sparepartshub.entity.SupplierProduct;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortalSupplierProductRepository extends JpaRepository<SupplierProduct,Integer>{@EntityGraph(attributePaths={"product","supplier"})List<SupplierProduct> findBySupplier_SupplierIdOrderByProduct_Name(Integer supplierId);Optional<SupplierProduct> findBySupplier_SupplierIdAndProduct_ProductId(Integer sid, Integer pid);}
