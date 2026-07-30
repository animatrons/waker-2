package com.waker.penalty;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PenaltyDispatch {

  private final Map<PenaltyType, PenaltyHandler> handlers;

  PenaltyDispatch(Map<PenaltyType, PenaltyHandler> handlers) {
    this.handlers = Map.copyOf(handlers);
  }

  public static PenaltyDispatch fromHandlers(List<PenaltyHandler> handlers) {
    EnumMap<PenaltyType, PenaltyHandler> map = new EnumMap<>(PenaltyType.class);
    for (PenaltyHandler handler : handlers) {
      PenaltyType type = handler.penaltyType();
      if (map.containsKey(type)) {
        throw new IllegalStateException("Duplicate PenaltyHandler for " + type);
      }
      map.put(type, handler);
    }
    for (PenaltyType type : PenaltyType.values()) {
      if (!map.containsKey(type)) {
        throw new IllegalStateException("Missing PenaltyHandler for " + type);
      }
    }
    return new PenaltyDispatch(map);
  }

  public PenaltyHandler handlerFor(PenaltyType type) {
    return handlers.get(type);
  }

  public void validateConfig(PenaltyConfig config) {
    PenaltyType type =
        switch (config) {
          case EmailToContactPenaltyConfig ignored -> PenaltyType.EMAIL_TO_CONTACT;
          case LeaderboardPenaltyConfig ignored -> PenaltyType.LEADERBOARD;
        };
    handlers.get(type).validateConfig(config);
  }
}
