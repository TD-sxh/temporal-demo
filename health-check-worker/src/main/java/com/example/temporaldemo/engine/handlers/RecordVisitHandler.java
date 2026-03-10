package com.example.temporaldemo.engine.handlers;

import com.example.temporaldemo.engine.activity.ActivityHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Activity handler: records a patient visit.
 * Registered name: "recordVisit"
 */
public class RecordVisitHandler implements ActivityHandler {

    private static final Logger logger = LoggerFactory.getLogger(RecordVisitHandler.class);

    @Override
    public Object handle(Map<String, Object> input) {
        String patientId = (String) input.get("patientId");
        String patientName = (String) input.get("patientName");
        String doctorName = (String) input.get("doctorName");
        String visitReason = (String) input.get("visitReason");

        String visitId = "VISIT-" + System.currentTimeMillis();
        logger.info("[recordVisit] Recorded visit {} for patient {} ({}) with {}. Reason: {}",
                visitId, patientId, patientName, doctorName, visitReason);
        return visitId;
    }
}
