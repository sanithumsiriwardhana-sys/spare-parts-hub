package com.sliit.sparepartshub.web;

import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.stockmonitoring.dto.CategoryStockSummary;
import com.sliit.sparepartshub.stockmonitoring.dto.UrgencyLevel;
import com.sliit.sparepartshub.stockmonitoring.repository.ProductRepository;
import com.sliit.sparepartshub.stockmonitoring.service.RestockSuggestionService;
import com.sliit.sparepartshub.stockmonitoring.service.StockRequestService;
import com.sliit.sparepartshub.stockmonitoring.service.UrgencyScoreService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PageController {

    // NOTE for teammates adding your own widget: this couples the shared
    // web package to one function's services (stockmonitoring), which
    // breaks the usual "package by feature" boundary - deliberate
    // tradeoff for a small landing-page widget, not a pattern to repeat
    // freely. Kept on feature/urgency-tracking, not main, until Function
    // 3 as a whole is merged - see the commit message for why. If you're
    // adding a widget for your own module, inject your own service the
    // same way, gate your section of dashboard.html with sec:authorize
    // for your role(s), and flag it in the team chat before editing this
    // file - multiple people editing the same controller/template on
    // different branches is exactly the kind of shared-file conflict
    // CONTRIBUTING.md warns about.
    private final UrgencyScoreService urgencyScoreService;
    private final RestockSuggestionService restockSuggestionService;
    private final StockRequestService stockRequestService;
    private final ProductRepository productRepository;

    public PageController(UrgencyScoreService urgencyScoreService,
                          RestockSuggestionService restockSuggestionService,
                          StockRequestService stockRequestService,
                          ProductRepository productRepository) {
        this.urgencyScoreService = urgencyScoreService;
        this.restockSuggestionService = restockSuggestionService;
        this.stockRequestService = stockRequestService;
        this.productRepository = productRepository;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Product> products = urgencyScoreService.getDashboardOrderedByUrgency();

        long criticalCount = products.stream()
                .filter(p -> urgencyScoreService.classify(p.getUrgencyScore()) == UrgencyLevel.CRITICAL)
                .count();
        long warningCount = products.stream()
                .filter(p -> urgencyScoreService.classify(p.getUrgencyScore()) == UrgencyLevel.WARNING)
                .count();

        int pendingRestockCount = restockSuggestionService.getPending().size();
        int pendingStockRequestCount = stockRequestService.getPending().size();

        List<CategoryStockSummary> categoryStockSummaries = productRepository.getStockCountByCategory();

        model.addAttribute("criticalCount", criticalCount);
        model.addAttribute("warningCount", warningCount);
        model.addAttribute("pendingRestockCount", pendingRestockCount);
        model.addAttribute("pendingStockRequestCount", pendingStockRequestCount);
        model.addAttribute("categoryStockSummaries", categoryStockSummaries);
        return "dashboard";
    }
}
