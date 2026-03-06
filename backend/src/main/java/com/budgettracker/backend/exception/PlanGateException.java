package com.budgettracker.backend.exception;

/**
 * Thrown when a FREE-plan user attempts to access a PRO-only feature.
 * Maps to HTTP 402 Payment Required in GlobalExceptionHandler.
 */
public class PlanGateException extends RuntimeException {

    private final String feature;

    public PlanGateException(String feature) {
        super("'" + feature + "' requires a Pro subscription.");
        this.feature = feature;
    }

    public String getFeature() { return feature; }
}
