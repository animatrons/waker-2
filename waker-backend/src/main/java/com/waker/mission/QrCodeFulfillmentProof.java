package com.waker.mission;

/** Placeholder proof type — fulfillment verification implemented in Story 2.6. */
public record QrCodeFulfillmentProof(String scannedPayload) implements MissionFulfillmentProof {}
