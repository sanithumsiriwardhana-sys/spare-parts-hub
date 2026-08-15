package com.sliit.sparepartshub.inventory.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Function 1 - Inventory Storage Location Tracking
 * Actors: Warehouse Clerk, Senior Sales Executive
 * Covers: PBI-01 to PBI-04 (Sprint 2)
 *
 * Route access is already restricted in SecurityConfig to
 * WAREHOUSE_CLERK, SALES_EXEC, ADMIN - no need to duplicate that here.
 */
@Controller
public class InventoryController {

    @GetMapping("/inventory")
    public String index() {
        return "inventory/index";
    }
}
