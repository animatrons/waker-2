package com.waker.mission;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MissionDispatch {

  private final Map<MissionType, MissionHandler> handlers;

  MissionDispatch(Map<MissionType, MissionHandler> handlers) {
    this.handlers = Map.copyOf(handlers);
  }

  public static MissionDispatch fromHandlers(List<MissionHandler> handlers) {
    EnumMap<MissionType, MissionHandler> map = new EnumMap<>(MissionType.class);
    for (MissionHandler handler : handlers) {
      MissionType type = handler.missionType();
      if (map.containsKey(type)) {
        throw new IllegalStateException("Duplicate MissionHandler for " + type);
      }
      map.put(type, handler);
    }
    for (MissionType type : MissionType.values()) {
      if (!map.containsKey(type)) {
        throw new IllegalStateException("Missing MissionHandler for " + type);
      }
    }
    return new MissionDispatch(map);
  }

  public MissionHandler handlerFor(MissionType type) {
    return handlers.get(type);
  }

  public void validateConfig(MissionConfig config) {
    MissionType type =
        switch (config) {
          case QrCodeMissionConfig ignored -> MissionType.QR_CODE;
          case WritingTaskMissionConfig ignored -> MissionType.WRITING_TASK;
          case MathGameMissionConfig ignored -> MissionType.MATH_GAME;
        };
    handlers.get(type).validateConfig(config);
  }

  public MissionConfig prepareForPersist(MissionConfig config) {
    MissionType type =
        switch (config) {
          case QrCodeMissionConfig ignored -> MissionType.QR_CODE;
          case WritingTaskMissionConfig ignored -> MissionType.WRITING_TASK;
          case MathGameMissionConfig ignored -> MissionType.MATH_GAME;
        };
    return handlers.get(type).prepareForPersist(config);
  }
}
