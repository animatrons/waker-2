package com.waker.penalty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailToContactPenaltyConfig(
    @NotBlank @Email @Size(max = 320) String contactEmail,
    @NotBlank @Size(max = 2_000) String message)
    implements PenaltyConfig {}
