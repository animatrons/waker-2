package com.waker.mission;

public record MathGameMissionConfig(String problemStatement, String expectedAnswer)
    implements MissionConfig {}
