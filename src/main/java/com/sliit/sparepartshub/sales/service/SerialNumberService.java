package com.sliit.sparepartshub.sales.service;

import com.sliit.sparepartshub.entity.Sale;
import com.sliit.sparepartshub.entity.SerialNumber;
import com.sliit.sparepartshub.sales.dto.AvailableSerialRow;
import com.sliit.sparepartshub.sales.repository.SerialNumberRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A product is treated as serial-tracked if it currently has any
 * serial_number rows with current_status = 'in_stock' - there's no flag
 * on Product for this (schema.sql has no such column, and adding one is
 * a schema change outside sales' authority to make unilaterally). This
 * is a pragmatic, data-driven substitute: cables/adapters never get
 * serial rows created for them, so they naturally never trigger this.
 * The trade-off: a serial-tracked product with zero current in-stock
 * rows (e.g. nothing received yet) is treated as non-serialized for
 * that sale - acceptable since the stock-count check should already
 * block selling something with no stock anyway.
 */
@Service
public class SerialNumberService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final SerialNumberRepository serialNumberRepository;

    public SerialNumberService(SerialNumberRepository serialNumberRepository) {
        this.serialNumberRepository = serialNumberRepository;
    }

    // Powers the Add to Cart serial picker. Oldest received first (FIFO)
    // - reasonable default for physical stock rotation, nothing in the
    // requirements demands a specific order.
    public List<AvailableSerialRow> getAvailableSerials(Integer productId) {
        return serialNumberRepository.findByProduct_ProductIdAndCurrentStatus(productId, SerialNumber.CurrentStatus.in_stock)
                .stream()
                .sorted(Comparator.comparing(SerialNumber::getReceivedDate))
                .map(sn -> new AvailableSerialRow(
                        sn.getSerialId(), sn.getSerialValue(),
                        sn.getReceivedDate() != null ? sn.getReceivedDate().format(DATE_FORMAT) : "-"))
                .collect(Collectors.toList());
    }

    // Called from CartService.addItem. Returns an empty list (no error)
    // if the product isn't serial-tracked at all, regardless of what was
    // requested - a non-serialized product should never be blocked by
    // this check.
    public List<SerialNumber> validateSelection(Integer productId, List<Integer> requestedSerialIds,
                                                 int expectedQuantity, Set<Integer> alreadyReservedIds) {
        List<Integer> ids = requestedSerialIds == null ? List.of() : requestedSerialIds;

        List<SerialNumber> available = serialNumberRepository
                .findByProduct_ProductIdAndCurrentStatus(productId, SerialNumber.CurrentStatus.in_stock);
        if (available.isEmpty()) {
            return List.of(); // not a serial-tracked product
        }

        if (ids.size() != expectedQuantity) {
            throw new SerialSelectionException(
                    "Please select exactly " + expectedQuantity + " serial number(s) for this item.");
        }

        Map<Integer, SerialNumber> byId = available.stream()
                .collect(Collectors.toMap(SerialNumber::getSerialId, sn -> sn));

        List<SerialNumber> selected = new ArrayList<>();
        for (Integer id : ids) {
            SerialNumber sn = byId.get(id);
            if (sn == null) {
                throw new SerialSelectionException(
                        "One or more selected serial numbers are no longer available - please try again.");
            }
            if (alreadyReservedIds.contains(id)) {
                throw new SerialSelectionException(
                        "Serial number " + sn.getSerialValue() + " is already selected elsewhere in this cart.");
            }
            selected.add(sn);
        }
        return selected;
    }

    // Called from CheckoutService before the sale is persisted - what
    // was true when the item was added to cart might not be true
    // anymore (someone else bought that exact unit, a stock correction
    // happened, etc.).
    public List<SerialNumber> revalidateAtCheckout(Integer productId, List<Integer> serialIds) {
        List<SerialNumber> found = serialNumberRepository.findAllById(serialIds);
        if (found.size() != serialIds.size()) {
            throw new CheckoutValidationException(
                    "One or more serial numbers for this sale could not be found - please remove and re-add the affected item.");
        }
        for (SerialNumber sn : found) {
            if (!sn.getProduct().getProductId().equals(productId)) {
                throw new CheckoutValidationException(
                        "A selected serial number does not match the product in your cart - please remove and re-add the affected item.");
            }
            if (sn.getCurrentStatus() != SerialNumber.CurrentStatus.in_stock) {
                throw new CheckoutValidationException(
                        "Serial number " + sn.getSerialValue() + " is no longer available - please remove and re-add the affected item.");
            }
        }
        return found;
    }

    // UC-02 postcondition 3: link the sold units to the sale.
    public void markSold(List<SerialNumber> serials, Sale sale) {
        for (SerialNumber sn : serials) {
            sn.setSale(sale);
            sn.setCurrentStatus(SerialNumber.CurrentStatus.sold);
            serialNumberRepository.save(sn);
        }
    }
}
