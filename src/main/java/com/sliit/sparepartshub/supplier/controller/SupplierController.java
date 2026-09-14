package com.sliit.sparepartshub.supplier.controller;

import com.sliit.sparepartshub.entity.PartnershipRequest;
import com.sliit.sparepartshub.entity.PurchaseOrder;
import com.sliit.sparepartshub.entity.PurchaseOrderItem;
import com.sliit.sparepartshub.entity.RestockSuggestion;
import com.sliit.sparepartshub.entity.Supplier;
import com.sliit.sparepartshub.security.CustomUserPrincipal;
import com.sliit.sparepartshub.supplier.dto.CreatePurchaseOrderForm;
import com.sliit.sparepartshub.supplier.dto.ReceiveShipmentForm;
import com.sliit.sparepartshub.supplier.dto.SupplierComparisonRow;
import com.sliit.sparepartshub.supplier.service.SupplierManagementService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/supplier")
public class SupplierController {

    private final SupplierManagementService service;

    public SupplierController(SupplierManagementService service) {
        this.service = service;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("supplierCount", service.getSuppliers().size());
        model.addAttribute("partnershipCount", service.getPartnershipRequests().stream()
                .filter(r -> r.getStatus() == PartnershipRequest.Status.pending).count());
        model.addAttribute("restockCount", service.getApprovedRestockSuggestions().size());
        model.addAttribute("poCount", service.getPurchaseOrders().size());
        return "supplier/index";
    }

    @GetMapping("/records")
    public String supplierRecords(Model model) {
        model.addAttribute("suppliers", service.getSuppliers());
        return "supplier/suppliers";
    }

    @GetMapping("/records/{id}/edit")
    public String editSupplier(@PathVariable Integer id, Model model) {
        model.addAttribute("supplier", service.getSupplier(id));
        return "supplier/supplier-form";
    }

    @PostMapping("/records/{id}")
    public String updateSupplier(@PathVariable Integer id,
                                 @RequestParam(required = false) String supplierCode,
                                 @RequestParam String name,
                                 @RequestParam String contact,
                                 @RequestParam String email,
                                 @AuthenticationPrincipal CustomUserPrincipal principal,
                                 RedirectAttributes ra) {
        try {
            service.updateSupplier(id, supplierCode, name, contact, email, principal.getUser());
            ra.addFlashAttribute("success", "Supplier record updated.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/supplier/records";
    }

    @GetMapping("/partnerships")
    public String partnerships(Model model) {
        model.addAttribute("requests", service.getPartnershipRequests());
        return "supplier/partnership-requests";
    }

    @PostMapping("/partnerships/{id}/decision")
    public String partnershipDecision(@PathVariable Integer id,
                                      @RequestParam PartnershipRequest.Status decision,
                                      @AuthenticationPrincipal CustomUserPrincipal principal,
                                      RedirectAttributes ra) {
        try {
            service.decidePartnership(id, decision, principal.getUser());
            ra.addFlashAttribute("success", "Partnership request " + decision.name() + ".");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/supplier/partnerships";
    }

    @GetMapping("/restock-requirements")
    public String restockRequirements(Model model) {
        model.addAttribute("suggestions", service.getApprovedRestockSuggestions());
        return "supplier/restock-requirements";
    }

    @GetMapping("/compare")
    public String compare(@RequestParam Integer suggestionId,
                          @RequestParam(required = false) Integer quantity,
                          Model model) {
        RestockSuggestion suggestion = service.getRestockSuggestion(suggestionId);
        int q = quantity == null ? suggestion.getSuggestedQuantity() : quantity;
        List<SupplierComparisonRow> rows = service.compareSuppliers(suggestion.getProduct().getProductId(), q);
        model.addAttribute("suggestion", suggestion);
        model.addAttribute("quantity", q);
        model.addAttribute("rows", rows);
        return "supplier/compare";
    }

    @GetMapping("/purchase-orders/new")
    public String newPurchaseOrder(@RequestParam Integer supplierId,
                                   @RequestParam Integer suggestionId,
                                   @RequestParam Integer quantity,
                                   @RequestParam BigDecimal price,
                                   Model model) {
        RestockSuggestion suggestion = service.getRestockSuggestion(suggestionId);
        Supplier supplier = service.getSupplier(supplierId);
        CreatePurchaseOrderForm form = new CreatePurchaseOrderForm();
        form.setSupplierId(supplierId);
        form.setProductId(suggestion.getProduct().getProductId());
        form.setQuantity(quantity);
        form.setAgreedPrice(price);
        form.setSuggestionId(suggestionId);
        model.addAttribute("form", form);
        model.addAttribute("supplier", supplier);
        model.addAttribute("suggestion", suggestion);
        return "supplier/purchase-order-form";
    }

    @PostMapping("/purchase-orders")
    public String createPurchaseOrder(@ModelAttribute("form") CreatePurchaseOrderForm form,
                                      @AuthenticationPrincipal CustomUserPrincipal principal,
                                      RedirectAttributes ra) {
        try {
            PurchaseOrder po = service.createPurchaseOrder(form, principal.getUser());
            ra.addFlashAttribute("success", "Purchase order " + po.getPoCode() + " created.");
            return "redirect:/supplier/purchase-orders/" + po.getPoId();
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/supplier/restock-requirements";
        }
    }

    @GetMapping("/purchase-orders")
    public String purchaseOrders(Model model) {
        model.addAttribute("purchaseOrders", service.getPurchaseOrders());
        return "supplier/purchase-orders";
    }

    @GetMapping("/purchase-orders/{id}")
    public String purchaseOrderDetail(@PathVariable Integer id, Model model) {
        PurchaseOrder po = service.getPurchaseOrder(id);
        List<PurchaseOrderItem> items = service.getPurchaseOrderItems(id);
        BigDecimal total = items.stream()
                .map(i -> i.getPriceAgreed().multiply(BigDecimal.valueOf(i.getQuantityOrdered())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("po", po);
        model.addAttribute("items", items);
        model.addAttribute("total", total);
        return "supplier/purchase-order-detail";
    }

    @PostMapping("/purchase-orders/{id}/ship")
    public String markShipped(@PathVariable Integer id,
                              @AuthenticationPrincipal CustomUserPrincipal principal,
                              RedirectAttributes ra) {
        try {
            service.markShipped(id, principal.getUser());
            ra.addFlashAttribute("success", "Purchase order marked as shipped.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/supplier/purchase-orders/" + id;
    }

    @GetMapping("/purchase-orders/{id}/receive")
    public String receiveForm(@PathVariable Integer id, Model model) {
        PurchaseOrder po = service.getPurchaseOrder(id);
        List<PurchaseOrderItem> items = service.getPurchaseOrderItems(id);
        model.addAttribute("po", po);
        model.addAttribute("items", items);
        model.addAttribute("form", new ReceiveShipmentForm());
        return "supplier/receive-shipment";
    }

    @PostMapping("/purchase-orders/{id}/receive")
    public String receive(@PathVariable Integer id,
                          @RequestParam Integer poItemId,
                          @ModelAttribute ReceiveShipmentForm form,
                          @AuthenticationPrincipal CustomUserPrincipal principal,
                          RedirectAttributes ra) {
        try {
            service.receiveSingleItemShipment(id, poItemId, form, principal.getUser());
            ra.addFlashAttribute("success", "Shipment receipt recorded and stock updated.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/supplier/purchase-orders/" + id;
    }

    @GetMapping("/purchase-orders/{id}/export.csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable Integer id) {
        PurchaseOrder po = service.getPurchaseOrder(id);
        List<PurchaseOrderItem> items = service.getPurchaseOrderItems(id);
        StringBuilder csv = new StringBuilder("PO Code,Supplier,Product,Quantity,Unit Price,Line Total,Status\n");
        for (PurchaseOrderItem item : items) {
            BigDecimal line = item.getPriceAgreed().multiply(BigDecimal.valueOf(item.getQuantityOrdered()));
            csv.append(cell(po.getPoCode())).append(',')
                    .append(cell(po.getSupplier().getName())).append(',')
                    .append(cell(item.getProduct().getName())).append(',')
                    .append(item.getQuantityOrdered()).append(',')
                    .append(item.getPriceAgreed()).append(',')
                    .append(line).append(',')
                    .append(po.getStatus()).append('\n');
        }
        byte[] data = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + po.getPoCode() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }

    private String cell(String value) {
        String escaped = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
