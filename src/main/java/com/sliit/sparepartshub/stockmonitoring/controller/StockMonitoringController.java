package com.sliit.sparepartshub.stockmonitoring.controller;

import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.stockmonitoring.dto.ProductUrgencyView;
import com.sliit.sparepartshub.stockmonitoring.dto.UrgencyLevel;
import com.sliit.sparepartshub.stockmonitoring.service.UrgencyScoreService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Function 3 - Dynamic Urgency Score Tracking
 * Actor: Inventory Supervisor
 * Covers: SP2-06 (urgency dashboard). Critical alerts (SP2-05), stock
 * request logging (PBI-11), and restock suggestion approval (PBI-12)
 * are separate pages, not built yet.
 */
@Controller
public class StockMonitoringController {

    private final UrgencyScoreService urgencyScoreService;

    public StockMonitoringController(UrgencyScoreService urgencyScoreService) {
        this.urgencyScoreService = urgencyScoreService;
    }

    @GetMapping("/stockmonitoring")
    public String dashboard(Model model) {
        List<Product> products = urgencyScoreService.getDashboardOrderedByUrgency();

        List<ProductUrgencyView> rows = products.stream()
                .map(p -> new ProductUrgencyView(p, urgencyScoreService.classify(p.getUrgencyScore())))
                .toList();

        // Reuses the same classify() call already done above - just a
        // filtered view, not a separate calculation (SP2-05).
        List<ProductUrgencyView> criticalItems = rows.stream()
                .filter(row -> row.getLevel() == UrgencyLevel.CRITICAL)
                .toList();

        model.addAttribute("rows", rows);
        model.addAttribute("criticalItems", criticalItems);
        return "stockmonitoring/index";
    }
}