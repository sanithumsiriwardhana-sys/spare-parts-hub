package com.sliit.sparepartshub.stockmonitoring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Function 3 - Dynamic Urgency Score Tracking
 * Actor: Inventory Supervisor
 * Covers: PBI-09 to PBI-12 (Sprint 2/3)
 */
@Controller
public class StockMonitoringController {

    @GetMapping("/stockmonitoring")
    public String index() {
        return "stockmonitoring/index";
    }
}
