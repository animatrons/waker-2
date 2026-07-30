package com.waker.mission;

import jakarta.validation.constraints.NotBlank;

public record MathGameMissionConfig(
    @NotBlank String problemStatement, @NotBlank String expectedAnswer) implements MissionConfig {}
