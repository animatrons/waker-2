package com.waker.penalty;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_class")
@JsonSubTypes({
  @JsonSubTypes.Type(value = EmailToContactPenaltyConfig.class, name = "EMAIL_TO_CONTACT"),
  @JsonSubTypes.Type(value = LeaderboardPenaltyConfig.class, name = "LEADERBOARD")
})
public sealed interface PenaltyConfig
    permits EmailToContactPenaltyConfig, LeaderboardPenaltyConfig {}
