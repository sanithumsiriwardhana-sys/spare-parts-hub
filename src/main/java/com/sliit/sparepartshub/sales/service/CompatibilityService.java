package com.sliit.sparepartshub.sales.service;

import com.sliit.sparepartshub.entity.CompatibilityRule;
import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.ProductSpec;
import com.sliit.sparepartshub.sales.dto.CompatibilityConflict;
import com.sliit.sparepartshub.sales.repository.CompatibilityRuleRepository;
import com.sliit.sparepartshub.sales.repository.ProductSpecRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * UC-02 step 6: "PC Builder Lite" - flags conflicts defined in
 * compatibility_rule (e.g. spec_type='ram_type', value_a='DDR5',
 * value_b='DDR4'). A rule matches in either direction.
 *
 * Two entry points:
 *  - checkAgainstCart: one new item vs. everything already in the cart,
 *    used on Add to Cart for an immediate warning.
 *  - checkCartConflicts: every pair in the final cart, used at checkout
 *    since removals/quantity edits since the last add could have
 *    changed the conflict set - the last thing shown to the Sales
 *    Executive before completing the sale has to reflect the cart as it
 *    actually is now, not as it was incrementally built up.
 */
@Service
public class CompatibilityService {

    private final ProductSpecRepository productSpecRepository;
    private final CompatibilityRuleRepository compatibilityRuleRepository;

    public CompatibilityService(ProductSpecRepository productSpecRepository,
                                 CompatibilityRuleRepository compatibilityRuleRepository) {
        this.productSpecRepository = productSpecRepository;
        this.compatibilityRuleRepository = compatibilityRuleRepository;
    }

    public List<CompatibilityConflict> checkAgainstCart(Product candidate, List<Product> existingCartProducts) {
        List<ProductSpec> candidateSpecs = productSpecRepository.findByProduct_ProductId(candidate.getProductId());
        if (candidateSpecs.isEmpty()) {
            return List.of();
        }

        List<Integer> otherProductIds = existingCartProducts.stream()
                .map(Product::getProductId)
                .filter(id -> !id.equals(candidate.getProductId()))
                .collect(Collectors.toList());
        if (otherProductIds.isEmpty()) {
            return List.of();
        }

        List<ProductSpec> existingSpecs = productSpecRepository.findByProduct_ProductIdIn(otherProductIds);
        if (existingSpecs.isEmpty()) {
            return List.of();
        }

        List<String> candidateSpecTypes = candidateSpecs.stream()
                .map(ProductSpec::getSpecType)
                .distinct()
                .collect(Collectors.toList());
        List<CompatibilityRule> rules = compatibilityRuleRepository.findBySpecTypeIn(candidateSpecTypes);
        if (rules.isEmpty()) {
            return List.of();
        }

        Map<Integer, Product> productsById = existingCartProducts.stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p, (a, b) -> a));

        List<CompatibilityConflict> conflicts = new ArrayList<>();
        for (ProductSpec candidateSpec : candidateSpecs) {
            for (ProductSpec existingSpec : existingSpecs) {
                if (!candidateSpec.getSpecType().equals(existingSpec.getSpecType())) {
                    continue;
                }
                for (CompatibilityRule rule : rules) {
                    if (!rule.getSpecType().equals(candidateSpec.getSpecType())) {
                        continue;
                    }
                    if (ruleMatches(rule, candidateSpec.getSpecValue(), existingSpec.getSpecValue())) {
                        Product existingProduct = productsById.get(existingSpec.getProduct().getProductId());
                        if (existingProduct != null) {
                            conflicts.add(new CompatibilityConflict(
                                    candidate.getProductId(), candidate.getName(),
                                    existingProduct.getProductId(), existingProduct.getName(),
                                    candidateSpec.getSpecType(), rule.getReason()));
                        }
                    }
                }
            }
        }
        return conflicts;
    }

    public List<CompatibilityConflict> checkCartConflicts(List<Product> cartProducts) {
        if (cartProducts.size() < 2) {
            return List.of();
        }

        List<Integer> productIds = cartProducts.stream().map(Product::getProductId).collect(Collectors.toList());
        List<ProductSpec> allSpecs = productSpecRepository.findByProduct_ProductIdIn(productIds);
        if (allSpecs.isEmpty()) {
            return List.of();
        }

        List<String> specTypes = allSpecs.stream().map(ProductSpec::getSpecType).distinct().collect(Collectors.toList());
        List<CompatibilityRule> rules = compatibilityRuleRepository.findBySpecTypeIn(specTypes);
        if (rules.isEmpty()) {
            return List.of();
        }

        Map<Integer, Product> productsById = cartProducts.stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p, (a, b) -> a));
        Map<Integer, List<ProductSpec>> specsByProduct = allSpecs.stream()
                .collect(Collectors.groupingBy(s -> s.getProduct().getProductId()));

        List<Integer> ids = new ArrayList<>(specsByProduct.keySet());
        List<CompatibilityConflict> conflicts = new ArrayList<>();

        for (int i = 0; i < ids.size(); i++) {
            for (int j = i + 1; j < ids.size(); j++) {
                Integer idA = ids.get(i);
                Integer idB = ids.get(j);
                for (ProductSpec specA : specsByProduct.get(idA)) {
                    for (ProductSpec specB : specsByProduct.get(idB)) {
                        if (!specA.getSpecType().equals(specB.getSpecType())) {
                            continue;
                        }
                        for (CompatibilityRule rule : rules) {
                            if (!rule.getSpecType().equals(specA.getSpecType())) {
                                continue;
                            }
                            if (ruleMatches(rule, specA.getSpecValue(), specB.getSpecValue())) {
                                conflicts.add(new CompatibilityConflict(
                                        idA, productsById.get(idA).getName(),
                                        idB, productsById.get(idB).getName(),
                                        specA.getSpecType(), rule.getReason()));
                            }
                        }
                    }
                }
            }
        }
        return conflicts;
    }

    private boolean ruleMatches(CompatibilityRule rule, String valueOne, String valueTwo) {
        return (rule.getValueA().equalsIgnoreCase(valueOne) && rule.getValueB().equalsIgnoreCase(valueTwo))
                || (rule.getValueA().equalsIgnoreCase(valueTwo) && rule.getValueB().equalsIgnoreCase(valueOne));
    }
}
