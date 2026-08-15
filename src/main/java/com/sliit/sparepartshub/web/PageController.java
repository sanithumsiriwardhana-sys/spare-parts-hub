package com.sliit.sparepartshub.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

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
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}
