package com.sliit.sparepartshub.stockmonitoring.scheduler;

import com.sliit.sparepartshub.stockmonitoring.service.UrgencyScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers UrgencyScoreService.recalculateAll() on a timer. Kept separate
 * from the service itself so the calculation logic doesn't depend on
 * "when" - a manual "Recalculate now" button (if we add one later) can
 * call the same service method directly without going through this class.
 *
 * cron = "0 0 1 * * *" -> once a day at 01:00 (seconds minutes hours
 * day-of-month month day-of-week). For demoing during development, where
 * waiting a full day to see updated scores isn't practical, temporarily
 * swap the annotation for fixedRate = 300000 (every 5 minutes, in ms) -
 * just remember to switch it back before submitting.
 */
@Component
public class UrgencyScoreScheduler {

    private static final Logger log = LoggerFactory.getLogger(UrgencyScoreScheduler.class);

    private final UrgencyScoreService urgencyScoreService;

    public UrgencyScoreScheduler(UrgencyScoreService urgencyScoreService) {
        this.urgencyScoreService = urgencyScoreService;
    }

    @Scheduled(fixedRate = 10000)
    public void recalculateUrgencyScores() {
        log.info("Running scheduled urgency score recalculation");
        urgencyScoreService.recalculateAll();
        log.info("Urgency score recalculation complete");
    }
}
