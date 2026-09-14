package com.sliit.sparepartshub.warranty.controller;

import com.sliit.sparepartshub.entity.RmaClaim;
import com.sliit.sparepartshub.entity.SerialNumber;
import com.sliit.sparepartshub.security.CustomUserPrincipal;
import com.sliit.sparepartshub.warranty.dto.RmaClaimCreateRequest;
import com.sliit.sparepartshub.warranty.dto.WarrantyDashboardMetrics;
import com.sliit.sparepartshub.warranty.dto.WarrantyValidationResult;
import com.sliit.sparepartshub.warranty.service.WarrantyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Function 4 - Warranty and Returns Management
 * Actor: Operations Coordinator (and Admin)
 * Covers: PBI-13 to PBI-16 (Sprint 3)
 */
@Controller
@RequestMapping("/warranty")
public class WarrantyController {

    private final WarrantyService warrantyService;

    public WarrantyController(WarrantyService warrantyService) {
        this.warrantyService = warrantyService;
    }

    /**
     * Dashboard view: KPIs, quick serial search, and filterable RMA claims list (PBI-16).
     */
    @GetMapping
    public String index(@RequestParam(required = false) RmaClaim.Resolution resolution,
                        @RequestParam(required = false) String search,
                        Model model) {
        WarrantyDashboardMetrics metrics = warrantyService.getDashboardMetrics();
        List<RmaClaim> claims = warrantyService.searchClaims(resolution, search);

        model.addAttribute("metrics", metrics);
        model.addAttribute("claims", claims);
        model.addAttribute("selectedResolution", resolution);
        model.addAttribute("search", search);
        model.addAttribute("resolutions", RmaClaim.Resolution.values());
        return "warranty/index";
    }

    /**
     * Serial number scanner and warranty coverage verification (PBI-13, PBI-14).
     */
    @GetMapping("/lookup")
    public String lookup(@RequestParam(required = false) String serialValue, Model model) {
        if (serialValue != null && !serialValue.trim().isEmpty()) {
            WarrantyValidationResult result = warrantyService.validateWarranty(serialValue.trim());
            model.addAttribute("result", result);
            model.addAttribute("serialValue", serialValue.trim());
        }
        return "warranty/lookup";
    }

    /**
     * Display the form to log a new RMA claim for an inspected serial (PBI-15).
     */
    @GetMapping("/claims/new")
    public String showCreateClaimForm(@RequestParam Integer serialId,
                                      RedirectAttributes redirectAttributes) {
        var serialOpt = warrantyService.getSerialById(serialId);
        if (serialOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Serial number not found.");
            return "redirect:/warranty/lookup";
        }
        return "redirect:/warranty/claims/create?serialValue=" + serialOpt.get().getSerialValue();
    }

    /**
     * Overloaded helper: Initiate claim creation directly from verified serial value.
     */
    @GetMapping("/claims/create")
    public String prepareClaimForm(@RequestParam String serialValue,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (serialValue == null || serialValue.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please provide a valid serial number.");
            return "redirect:/warranty/lookup";
        }

        WarrantyValidationResult validation = warrantyService.validateWarranty(serialValue.trim());
        if (validation.getStatus() == WarrantyValidationResult.CoverageStatus.NOT_FOUND) {
            redirectAttributes.addFlashAttribute("error", "Serial number '" + serialValue + "' was not found.");
            return "redirect:/warranty/lookup";
        }

        RmaClaimCreateRequest claimRequest = new RmaClaimCreateRequest();
        claimRequest.setSerialId(validation.getSerialNumber().getSerialId());
        claimRequest.setResolution(RmaClaim.Resolution.pending);

        model.addAttribute("validation", validation);
        model.addAttribute("serial", validation.getSerialNumber());
        model.addAttribute("claimRequest", claimRequest);
        model.addAttribute("resolutions", RmaClaim.Resolution.values());
        return "warranty/create-claim";
    }

    /**
     * Save new RMA claim (PBI-15, UC-04 step 5).
     */
    @PostMapping("/claims")
    public String createClaim(@ModelAttribute RmaClaimCreateRequest claimRequest,
                              @AuthenticationPrincipal CustomUserPrincipal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            RmaClaim savedClaim = warrantyService.createClaim(claimRequest, principal.getUser());
            redirectAttributes.addFlashAttribute("message", "RMA Claim " + savedClaim.getClaimCode() + " logged successfully.");
            return "redirect:/warranty/claims/" + savedClaim.getClaimId();
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/warranty/lookup";
        }
    }

    /**
     * Detailed view of a single RMA claim with resolution update actions (PBI-15, PBI-16).
     */
    @GetMapping("/claims/{id}")
    public String claimDetail(@PathVariable Integer id, Model model) {
        RmaClaim claim = warrantyService.getClaimById(id)
                .orElseThrow(() -> new IllegalArgumentException("RMA Claim not found with ID: " + id));

        // Also fetch any other claims for the same serial unit
        List<RmaClaim> serialHistory = warrantyService.getClaimsForSerial(claim.getSerial().getSerialId());

        model.addAttribute("claim", claim);
        model.addAttribute("serialHistory", serialHistory);
        model.addAttribute("resolutions", RmaClaim.Resolution.values());
        return "warranty/claim-detail";
    }

    /**
     * Update resolution status of an existing claim (PBI-15).
     */
    @PostMapping("/claims/{id}/resolve")
    public String updateResolution(@PathVariable Integer id,
                                   @RequestParam RmaClaim.Resolution resolution,
                                   @RequestParam(required = false) String additionalNotes,
                                   @AuthenticationPrincipal CustomUserPrincipal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            warrantyService.updateResolution(id, resolution, additionalNotes, principal.getUser());
            redirectAttributes.addFlashAttribute("message", "Claim resolution updated to " + resolution + ".");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/warranty/claims/" + id;
    }

    /**
     * View complete lifecycle & RMA history of a serial number (PBI-16).
     */
    @GetMapping("/serials/{serialValue}/history")
    public String serialHistory(@PathVariable String serialValue, Model model, RedirectAttributes redirectAttributes) {
        var serialOpt = warrantyService.getSerialByValue(serialValue);
        if (serialOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Serial number not found: " + serialValue);
            return "redirect:/warranty";
        }

        SerialNumber serial = serialOpt.get();
        List<RmaClaim> claims = warrantyService.getClaimsForSerial(serial.getSerialId());

        model.addAttribute("serial", serial);
        model.addAttribute("claims", claims);
        return "warranty/serial-history";
    }
}
