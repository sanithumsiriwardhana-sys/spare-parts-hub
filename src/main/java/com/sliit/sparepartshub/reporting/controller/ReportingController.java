package com.sliit.sparepartshub.reporting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Function 6 - Centralized Reporting, Audit Logging & External Supplier Portal
 * Actors: Shop Owner / Admin (internal), Supplier (external)
 * Covers: PBI-19, PBI-21 to PBI-24 (Sprint 4)
 *
 * Note: the Supplier-facing portal will need its own authentication path
 * separate from SecurityConfig's staff login, since Supplier isn't in the
 * users table (see the actor generalization diagram). That's a bigger
 * design decision for whoever picks up this module - flag it with the
 * team before building the portal login.
 */
@Controller
public class ReportingController {

    @GetMapping("/reporting")
    public String index() {
        return "reporting/index";
    }
}
