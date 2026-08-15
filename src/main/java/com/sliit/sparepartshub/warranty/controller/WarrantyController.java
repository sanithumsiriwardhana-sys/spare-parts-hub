package com.sliit.sparepartshub.warranty.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Function 4 - Warranty and Returns Management
 * Actor: Operations Coordinator
 * Covers: PBI-13 to PBI-16 (Sprint 3)
 */
@Controller
public class WarrantyController {

    @GetMapping("/warranty")
    public String index() {
        return "warranty/index";
    }
}
