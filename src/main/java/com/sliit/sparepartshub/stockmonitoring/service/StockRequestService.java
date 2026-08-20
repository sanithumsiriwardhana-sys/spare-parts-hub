package com.sliit.sparepartshub.stockmonitoring.service;

import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.StockRequest;
import com.sliit.sparepartshub.entity.User;
import com.sliit.sparepartshub.stockmonitoring.repository.StockRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Function 3's demand-tracking sub-function (PBI-11): log a customer's
 * interest in an out-of-stock item, notify them once it's back, and mark
 * the request fulfilled once they buy it. Three-stage lifecycle:
 * pending -> notified -> fulfilled.
 */
@Service
public class StockRequestService {

    private final StockRequestRepository stockRequestRepository;

    public StockRequestService(StockRequestRepository stockRequestRepository) {
        this.stockRequestRepository = stockRequestRepository;
    }

    public List<StockRequest> getPending() {
        return stockRequestRepository.findByStatus(StockRequest.Status.pending);
    }

    public List<StockRequest> getNotified() {
        return stockRequestRepository.findByStatus(StockRequest.Status.notified);
    }

    public void create(Product product, User loggedBy, String customerName,
                        String customerEmail, String customerPhonenum) {
        StockRequest request = new StockRequest();
        request.setProduct(product);
        request.setLoggedBy(loggedBy);
        request.setCustomerName(customerName);
        request.setCustomerEmail(customerEmail);
        request.setCustomerPhonenum(customerPhonenum);
        request.setStatus(StockRequest.Status.pending);
        stockRequestRepository.save(request);
    }

    /**
     * Staff has contacted the customer to say the item is back in stock.
     * Doesn't check current stock_count here - that's a judgement call
     * for the staff member marking it, not something to silently enforce
     * (e.g. they might notify slightly ahead of a shipment landing).
     */
    public void markNotified(Integer requestId) {
        StockRequest request = getOrThrow(requestId);
        if (request.getStatus() != StockRequest.Status.pending) {
            throw new IllegalStateException("Only a pending request can be marked as notified.");
        }
        request.setStatus(StockRequest.Status.notified);
        request.setNotifiedAt(LocalDateTime.now());
        stockRequestRepository.save(request);
    }

    /**
     * Customer has actually purchased the item. No fulfilledAt column
     * exists on stock_request (unlike notifiedAt) - only the status
     * changes here.
     */
    public void markFulfilled(Integer requestId) {
        StockRequest request = getOrThrow(requestId);
        if (request.getStatus() != StockRequest.Status.notified) {
            throw new IllegalStateException("Only a notified request can be marked as fulfilled.");
        }
        request.setStatus(StockRequest.Status.fulfilled);
        stockRequestRepository.save(request);
    }

    private StockRequest getOrThrow(Integer requestId) {
        return stockRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown stock request id: " + requestId));
    }
}
