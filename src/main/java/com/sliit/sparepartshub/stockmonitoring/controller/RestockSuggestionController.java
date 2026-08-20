package com.sliit.sparepartshub.stockmonitoring.controller;

import com.sliit.sparepartshub.entity.RestockSuggestion;
import com.sliit.sparepartshub.security.CustomUserPrincipal;
import com.sliit.sparepartshub.stockmonitoring.service.RestockSuggestionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Function 3 - restock suggestion approval queue (PBI-12). Suggestions
 * themselves are raised automatically by UrgencyScoreService whenever a
 * product goes CRITICAL; this controller only handles the Supervisor's
 * approve/modify/reject decision on ones already raised.
 */
@Controller
public class RestockSuggestionController {

    private final RestockSuggestionService restockSuggestionService;

    public RestockSuggestionController(RestockSuggestionService restockSuggestionService) {
        this.restockSuggestionService = restockSuggestionService;
    }

    @GetMapping("/stockmonitoring/restock-suggestions")
    public String list(Model model) {
        List<RestockSuggestion> pending = restockSuggestionService.getPending();
        model.addAttribute("pending", pending);
        return "stockmonitoring/restock-suggestions";
    }

    @PostMapping("/stockmonitoring/restock-suggestions/{id}/approve")
    public String approve(@PathVariable Integer id,
                           @AuthenticationPrincipal CustomUserPrincipal principal,
                           RedirectAttributes redirectAttributes) {
        try {
            restockSuggestionService.approve(id, principal.getUser());
            redirectAttributes.addFlashAttribute("message", "Suggestion approved.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stockmonitoring/restock-suggestions";
    }

    @PostMapping("/stockmonitoring/restock-suggestions/{id}/modify")
    public String modify(@PathVariable Integer id,
                          @RequestParam Integer quantity,
                          @RequestParam(required = false) String reason,
                          @AuthenticationPrincipal CustomUserPrincipal principal,
                          RedirectAttributes redirectAttributes) {
        try {
            restockSuggestionService.modify(id, quantity, reason, principal.getUser());
            redirectAttributes.addFlashAttribute("message", "Suggestion modified and approved.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stockmonitoring/restock-suggestions";
    }

    @PostMapping("/stockmonitoring/restock-suggestions/{id}/reject")
    public String reject(@PathVariable Integer id,
                          @RequestParam String reason,
                          @AuthenticationPrincipal CustomUserPrincipal principal,
                          RedirectAttributes redirectAttributes) {
        try {
            restockSuggestionService.reject(id, reason, principal.getUser());
            redirectAttributes.addFlashAttribute("message", "Suggestion rejected.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stockmonitoring/restock-suggestions";
    }
}
