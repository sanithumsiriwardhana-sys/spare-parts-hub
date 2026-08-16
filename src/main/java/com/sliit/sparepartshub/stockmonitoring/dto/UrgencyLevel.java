package com.sliit.sparepartshub.stockmonitoring.dto;

/**
 * Dashboard status bucket, driven by urgency_score rather than a flat
 * stock-count threshold. See UrgencyScoreService.classify().
 */
public enum UrgencyLevel {
    CRITICAL, WARNING, SAFE
}
