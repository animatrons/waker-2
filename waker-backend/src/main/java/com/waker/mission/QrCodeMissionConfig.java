package com.waker.mission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QrCodeMissionConfig(@NotBlank @Size(max = 512) String codePayload)
    implements MissionConfig {}
