package com.sliit.sparepartshub.sales.service;

import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.sales.dto.ProductSearchItem;
import com.sliit.sparepartshub.sales.dto.SearchFilterOptions;
import com.sliit.sparepartshub.sales.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UC-02 steps 1-3 (Real-Time Product Search). Compatibility checking and
 * checkout are separate concerns and belong in their own services once
 * built - this one only answers "what matches this search".
 */
@Service
public class ProductSearchService {

    private final ProductRepository productRepository;

    public ProductSearchService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Blank/whitespace-only filters are normalized to null so the
    // repository query's null-check branches handle them the same way -
    // an empty search box should behave like "no filter", not "match
    // nothing".
    public List<Product> search(String keyword, String category, String brand) {
        return productRepository.search(
                normalize(keyword), normalize(category), normalize(brand));
    }

    // Used by the instant-filter JSON endpoint (tabs/search-as-you-type) -
    // same query as search(), just mapped to the flat DTO so the entity
    // never has to be serialized directly.
    public List<ProductSearchItem> searchAsDto(String keyword, String category, String brand) {
        return search(keyword, category, brand).stream()
                .map(p -> new ProductSearchItem(
                        p.getProductId(), p.getName(), p.getCategory(),
                        p.getBrand(), p.getPrice(), p.getStockCount()))
                .collect(Collectors.toList());
    }

    public SearchFilterOptions getFilterOptions() {
        return new SearchFilterOptions(
                productRepository.findDistinctCategories(),
                productRepository.findDistinctBrands());
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
