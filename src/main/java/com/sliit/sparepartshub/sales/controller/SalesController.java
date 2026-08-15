package com.sliit.sparepartshub.sales.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Function 2 - Real-Time Product Search & Automated Checkout
 * Actor: Senior Sales Executive
 * Covers: PBI-05 to PBI-08 (Sprint 1)
 */
@Controller
public class SalesController {

    @GetMapping("/sales")
    public String index() {
        return "sales/index";
    }
}
