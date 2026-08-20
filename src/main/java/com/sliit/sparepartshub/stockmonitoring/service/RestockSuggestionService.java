package com.sliit.sparepartshub.stockmonitoring.service;

import com.sliit.sparepartshub.entity.RestockSuggestion;
import com.sliit.sparepartshub.entity.User;
import com.sliit.sparepartshub.stockmonitoring.repository.RestockSuggestionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The Inventory Supervisor's approve/modify/reject decision on a restock
 * suggestion (PBI-12, UC-03 postcondition 3). Kept separate from
 * UrgencyScoreService, which only creates suggestions - this class only
 * resolves them. Different actors, different lifecycle stage.
 */
@Service
public class RestockSuggestionService {

    private final RestockSuggestionRepository restockSuggestionRepository;

    public RestockSuggestionService(RestockSuggestionRepository restockSuggestionRepository) {
        this.restockSuggestionRepository = restockSuggestionRepository;
    }

    public List<RestockSuggestion> getPending() {
        return restockSuggestionRepository.findByStatus(RestockSuggestion.Status.pending);
    }

    public void approve(Integer suggestionId, User reviewer) {
        RestockSuggestion suggestion = getPendingOrThrow(suggestionId);
        suggestion.setStatus(RestockSuggestion.Status.approved);
        finishReview(suggestion, reviewer, null);
    }

    public void modify(Integer suggestionId, Integer newQuantity, String reason, User reviewer) {
        if (newQuantity == null || newQuantity <= 0) {
            throw new IllegalArgumentException("Modified quantity must be a positive number.");
        }
        RestockSuggestion suggestion = getPendingOrThrow(suggestionId);
        suggestion.setSuggestedQuantity(newQuantity);
        suggestion.setStatus(RestockSuggestion.Status.modified);
        finishReview(suggestion, reviewer, reason);
    }

    public void reject(Integer suggestionId, String reason, User reviewer) {
        // Enforced here, not at the entity level - a rejection without a
        // stated reason isn't useful to whoever reviews this decision
        // later (UC-03 step 9a).
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required when rejecting a restock suggestion.");
        }
        RestockSuggestion suggestion = getPendingOrThrow(suggestionId);
        suggestion.setStatus(RestockSuggestion.Status.rejected);
        finishReview(suggestion, reviewer, reason);
    }

    private RestockSuggestion getPendingOrThrow(Integer suggestionId) {
        RestockSuggestion suggestion = restockSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown restock suggestion id: " + suggestionId));
        if (suggestion.getStatus() != RestockSuggestion.Status.pending) {
            throw new IllegalStateException("This suggestion has already been reviewed.");
        }
        return suggestion;
    }

    private void finishReview(RestockSuggestion suggestion, User reviewer, String reason) {
        if (reason != null && !reason.isBlank()) {
            suggestion.setReason(reason);
        }
        suggestion.setReviewedBy(reviewer);
        suggestion.setReviewedAt(LocalDateTime.now());
        restockSuggestionRepository.save(suggestion);
    }
}
