package com.sliit.sparepartshub.reporting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Routes under /supplier-portal - authenticated via SupplierSecurityConfig,
 * entirely separate from staff auth in SecurityConfig. Keep this
 * controller distinct from ReportingController, which handles the
 * internal /reporting admin routes for this same function.
 */
@Controller
public class SupplierPortalController {

    @GetMapping("/supplier-portal/login")
    public String login() {
        return "supplier-portal/login";
    }

    @GetMapping("/supplier-portal/dashboard")
    public String dashboard() {
        return "supplier-portal/dashboard";
    }
}
