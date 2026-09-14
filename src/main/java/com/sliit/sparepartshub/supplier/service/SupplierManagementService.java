package com.sliit.sparepartshub.supplier.service;

import com.sliit.sparepartshub.entity.*;
import com.sliit.sparepartshub.reporting.repository.SupplierRepository;
import com.sliit.sparepartshub.stockmonitoring.repository.RestockSuggestionRepository;
import com.sliit.sparepartshub.supplier.dto.CreatePurchaseOrderForm;
import com.sliit.sparepartshub.supplier.dto.ReceiveShipmentForm;
import com.sliit.sparepartshub.supplier.dto.SupplierComparisonRow;
import com.sliit.sparepartshub.supplier.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SupplierManagementService {

    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;
    private final PartnershipRequestRepository partnershipRequestRepository;
    private final RestockOfferRepository restockOfferRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final ProductRepository productRepository;
    private final RestockSuggestionRepository restockSuggestionRepository;
    private final SerialNumberRepository serialNumberRepository;
    private final AuditLogRepository auditLogRepository;

    public SupplierManagementService(SupplierRepository supplierRepository,
                                     SupplierProductRepository supplierProductRepository,
                                     PartnershipRequestRepository partnershipRequestRepository,
                                     RestockOfferRepository restockOfferRepository,
                                     PurchaseOrderRepository purchaseOrderRepository,
                                     PurchaseOrderItemRepository purchaseOrderItemRepository,
                                     ProductRepository productRepository,
                                     RestockSuggestionRepository restockSuggestionRepository,
                                     SerialNumberRepository serialNumberRepository,
                                     AuditLogRepository auditLogRepository) {
        this.supplierRepository = supplierRepository;
        this.supplierProductRepository = supplierProductRepository;
        this.partnershipRequestRepository = partnershipRequestRepository;
        this.restockOfferRepository = restockOfferRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.productRepository = productRepository;
        this.restockSuggestionRepository = restockSuggestionRepository;
        this.serialNumberRepository = serialNumberRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<Supplier> getSuppliers() {
        return supplierRepository.findAll();
    }

    public Supplier getSupplier(Integer id) {
        return supplierRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Supplier not found."));
    }

    @Transactional
    public Supplier updateSupplier(Integer id, String supplierCode, String name, String contact, String email, User actor) {
        Supplier supplier = getSupplier(id);
        Map<String, Object> oldValue = Map.of(
                "supplierCode", safe(supplier.getSupplierCode()),
                "name", supplier.getName(),
                "contact", supplier.getContact(),
                "email", supplier.getEmail());

        supplier.setSupplierCode(blankToNull(supplierCode));
        supplier.setName(requireText(name, "Supplier name"));
        supplier.setContact(requireText(contact, "Contact"));
        supplier.setEmail(requireText(email, "Email"));
        Supplier saved = supplierRepository.save(supplier);

        audit(actor, "UPDATE", "supplier", saved.getSupplierId(), oldValue,
                Map.of("supplierCode", safe(saved.getSupplierCode()), "name", saved.getName(),
                        "contact", saved.getContact(), "email", saved.getEmail()));
        return saved;
    }

    public List<PartnershipRequest> getPartnershipRequests() {
        return partnershipRequestRepository.findAllByOrderBySubmittedAtDesc();
    }

    @Transactional
    public void decidePartnership(Integer requestId, PartnershipRequest.Status decision, User actor) {
        if (decision != PartnershipRequest.Status.approved && decision != PartnershipRequest.Status.rejected) {
            throw new IllegalArgumentException("Decision must be approved or rejected.");
        }
        PartnershipRequest request = partnershipRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Partnership request not found."));
        PartnershipRequest.Status old = request.getStatus();
        request.setStatus(decision);
        partnershipRequestRepository.save(request);
        audit(actor, "PARTNERSHIP_DECISION", "partnership_request", requestId,
                Map.of("status", old.name()), Map.of("status", decision.name()));
    }

    @Transactional(readOnly = true)
    public List<RestockSuggestion> getApprovedRestockSuggestions() {
        List<RestockSuggestion> all = new ArrayList<>();
        all.addAll(restockSuggestionRepository.findByStatus(RestockSuggestion.Status.approved));
        all.addAll(restockSuggestionRepository.findByStatus(RestockSuggestion.Status.modified));
        // initialize product while transaction is open for Thymeleaf
        all.forEach(s -> s.getProduct().getName());
        all.sort(Comparator.comparing(RestockSuggestion::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return all;
    }

    public RestockSuggestion getRestockSuggestion(Integer id) {
        return restockSuggestionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restock suggestion not found."));
    }

    @Transactional(readOnly = true)
    public List<SupplierComparisonRow> compareSuppliers(Integer productId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero.");
        List<SupplierProduct> terms = supplierProductRepository.findByProduct_ProductId(productId);
        List<RestockOffer> offers = restockOfferRepository.findByProduct_ProductIdOrderBySubmittedAtDesc(productId);
        Map<Integer, RestockOffer> newestOfferBySupplier = offers.stream()
                .collect(Collectors.toMap(o -> o.getSupplier().getSupplierId(), o -> o, (a, b) -> a));

        List<SupplierComparisonRow> rows = new ArrayList<>();
        for (SupplierProduct term : terms) {
            SupplierComparisonRow row = new SupplierComparisonRow();
            row.setSupplierId(term.getSupplier().getSupplierId());
            row.setSupplierName(term.getSupplier().getName());
            row.setMoq(term.getMoq());
            row.setLeadTimeDays(term.getLeadTimeDays());
            row.setEligible(quantity >= term.getMoq());

            BigDecimal effectivePrice = term.getPrice();
            RestockOffer offer = newestOfferBySupplier.get(term.getSupplier().getSupplierId());
            if (offer != null && offer.getQuantity() >= quantity && offer.getPrice().compareTo(effectivePrice) < 0) {
                effectivePrice = offer.getPrice();
                row.setOfferUsed(true);
                row.setOfferQuantity(offer.getQuantity());
            }
            row.setUnitPrice(effectivePrice);
            row.setTotalCost(effectivePrice.multiply(BigDecimal.valueOf(quantity)));
            rows.add(row);
        }

        rows.sort(Comparator
                .comparing(SupplierComparisonRow::isEligible).reversed()
                .thenComparing(SupplierComparisonRow::getTotalCost)
                .thenComparing(SupplierComparisonRow::getLeadTimeDays));
        rows.stream().filter(SupplierComparisonRow::isEligible).findFirst().ifPresent(r -> r.setRecommended(true));
        return rows;
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(CreatePurchaseOrderForm form, User actor) {
        if (form.getSupplierId() == null || form.getProductId() == null || form.getQuantity() == null || form.getAgreedPrice() == null) {
            throw new IllegalArgumentException("Supplier, product, quantity and agreed price are required.");
        }
        if (form.getQuantity() <= 0 || form.getAgreedPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity and agreed price must be positive.");
        }

        Supplier supplier = getSupplier(form.getSupplierId());
        Product product = productRepository.findById(form.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
        SupplierProduct terms = supplierProductRepository
                .findBySupplier_SupplierIdAndProduct_ProductId(supplier.getSupplierId(), product.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("The selected supplier has no terms for this product."));
        if (form.getQuantity() < terms.getMoq()) {
            throw new IllegalArgumentException("Quantity is below this supplier's MOQ of " + terms.getMoq() + ".");
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(supplier);
        po.setCreatedBy(actor);
        po.setStatus(PurchaseOrder.Status.pending);
        po = purchaseOrderRepository.save(po);
        po.setPoCode("PO-" + String.format("%06d", po.getPoId()));
        po = purchaseOrderRepository.save(po);

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseOrder(po);
        item.setProduct(product);
        item.setQuantityOrdered(form.getQuantity());
        item.setPriceAgreed(form.getAgreedPrice());
        purchaseOrderItemRepository.save(item);

        audit(actor, "CREATE_PO", "purchase_order", po.getPoId(), null,
                Map.of("poCode", po.getPoCode(), "supplierId", supplier.getSupplierId(), "productId", product.getProductId(),
                        "quantity", form.getQuantity(), "priceAgreed", form.getAgreedPrice(), "status", po.getStatus().name()));
        return po;
    }

    public List<PurchaseOrder> getPurchaseOrders() {
        return purchaseOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    public PurchaseOrder getPurchaseOrder(Integer id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found."));
    }

    public List<PurchaseOrderItem> getPurchaseOrderItems(Integer poId) {
        return purchaseOrderItemRepository.findByPurchaseOrder_PoId(poId);
    }

    @Transactional
    public void markShipped(Integer poId, User actor) {
        PurchaseOrder po = getPurchaseOrder(poId);
        if (po.getStatus() != PurchaseOrder.Status.pending) {
            throw new IllegalArgumentException("Only a pending purchase order can be marked shipped.");
        }
        PurchaseOrder.Status old = po.getStatus();
        po.setStatus(PurchaseOrder.Status.shipped);
        purchaseOrderRepository.save(po);
        audit(actor, "PO_STATUS", "purchase_order", poId,
                Map.of("status", old.name()), Map.of("status", po.getStatus().name()));
    }

    @Transactional
    public void receiveSingleItemShipment(Integer poId, Integer poItemId, ReceiveShipmentForm form, User actor) {
        PurchaseOrder po = getPurchaseOrder(poId);
        if (po.getStatus() == PurchaseOrder.Status.received) {
            throw new IllegalArgumentException("This purchase order is already fully received.");
        }
        if (po.getStatus() != PurchaseOrder.Status.shipped && po.getStatus() != PurchaseOrder.Status.partially_received) {
            throw new IllegalArgumentException("The purchase order must be shipped before it can be received.");
        }
        PurchaseOrderItem item = purchaseOrderItemRepository.findById(poItemId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order item not found."));
        if (!item.getPurchaseOrder().getPoId().equals(poId)) {
            throw new IllegalArgumentException("Purchase order item does not belong to this order.");
        }
        int accepted = form.getAcceptedQuantity() == null ? 0 : form.getAcceptedQuantity();
        if (accepted < 0 || accepted > item.getQuantityOrdered()) {
            throw new IllegalArgumentException("Accepted quantity must be between 0 and " + item.getQuantityOrdered() + ".");
        }

        List<String> serials = parseSerials(form.getSerialNumbers());
        if (serials.size() > accepted) {
            throw new IllegalArgumentException("Serial-number count cannot exceed accepted quantity.");
        }
        for (String serial : serials) {
            if (serialNumberRepository.existsBySerialValue(serial)) {
                throw new IllegalArgumentException("Duplicate serial number: " + serial);
            }
        }

        Product product = productRepository.findById(item.getProduct().getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
        int oldStock = product.getStockCount();
        product.setStockCount(oldStock + accepted);
        productRepository.save(product);

        for (String serial : serials) {
            SerialNumber sn = new SerialNumber();
            sn.setProduct(product);
            sn.setPurchaseOrderItem(item);
            sn.setSerialValue(serial);
            sn.setCurrentStatus(SerialNumber.CurrentStatus.in_stock);
            sn.setReceivedDate(LocalDate.now());
            serialNumberRepository.save(sn);
        }

        PurchaseOrder.Status oldStatus = po.getStatus();
        if (accepted == item.getQuantityOrdered()) {
            po.setStatus(PurchaseOrder.Status.received);
            po.setReceivedAt(LocalDateTime.now());
        } else {
            po.setStatus(PurchaseOrder.Status.partially_received);
        }
        purchaseOrderRepository.save(po);

        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("poItemId", poItemId);
        receipt.put("productId", product.getProductId());
        receipt.put("orderedQuantity", item.getQuantityOrdered());
        receipt.put("acceptedQuantity", accepted);
        receipt.put("serialNumbers", serials);
        receipt.put("discrepancyNote", safe(form.getDiscrepancyNote()));
        receipt.put("stockBefore", oldStock);
        receipt.put("stockAfter", product.getStockCount());
        receipt.put("poStatus", po.getStatus().name());
        audit(actor, "RECEIVE_SHIPMENT", "purchase_order", poId,
                Map.of("status", oldStatus.name(), "stock", oldStock), receipt);
    }

    private List<String> parseSerials(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.split("[,\\r\\n]+"))
                .map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
    }

    private void audit(User actor, String actionType, String tableName, Integer recordId,
                       Object oldValue, Object newValue) {
        AuditLog log = new AuditLog();
        log.setUser(actor);
        log.setActionType(actionType);
        log.setTableName(tableName);
        log.setRecordId(recordId);
        log.setOldValue(toJson(oldValue));
        log.setNewValue(toJson(newValue));
        auditLogRepository.save(log);
    }

    private String toJson(Object value) {
        if (value == null) return null;
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(e -> quote(String.valueOf(e.getKey())) + ":" + jsonValue(e.getValue()))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        return jsonValue(value);
    }

    private String jsonValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::jsonValue).collect(Collectors.joining(",", "[", "]"));
        }
        return quote(String.valueOf(value));
    }

    private String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        return value.trim();
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String safe(String value) { return value == null ? "" : value; }
}
