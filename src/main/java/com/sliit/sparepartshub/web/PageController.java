package com.sliit.sparepartshub.web;

import com.sliit.sparepartshub.entity.User;
import com.sliit.sparepartshub.sales.service.SalesDashboardService;
import com.sliit.sparepartshub.security.CustomUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final SalesDashboardService salesDashboardService;

    public PageController(SalesDashboardService salesDashboardService) {
        this.salesDashboardService = salesDashboardService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Placeholder landing page after login. Each member's module can add
    // its own dashboard widget/link here later - keep this controller as
    // the one shared entry point rather than duplicating "/dashboard"
    // mappings elsewhere.
    //
    // Function 2's block: only computed for roles that'll actually see
    // it (dashboard.html gates the widget itself to SALES_EXEC/ADMIN via
    // sec:authorize) - no reason to run the sales aggregation queries on
    // every dashboard load for a Warehouse Clerk who'll never see the
    // result.
    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal CustomUserPrincipal principal) {
        if (principal != null && hasSalesDashboardAccess(principal.getUser())) {
            model.addAttribute("salesStats", salesDashboardService.getStats());
        }
        return "dashboard";
    }

    private boolean hasSalesDashboardAccess(User user) {
        return user.getRole() == User.Role.sales_exec || user.getRole() == User.Role.admin;
    }
}