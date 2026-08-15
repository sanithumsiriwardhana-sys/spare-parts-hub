package com.sliit.sparepartshub.supplier.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Function 5 - Supplier Management
 * Actor: Shop Owner / Admin
 * Covers: PBI-17, PBI-18 (Sprint 3)
 */
@Controller
public class SupplierController {

    @GetMapping("/supplier")
    public String index() {
        return "supplier/index";
    }
}
