package com.waker.mission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QrCodeFulfillmentProof(@NotBlank @Size(max = 512) String scannedPayload)
    implements MissionFulfillmentProof {}
