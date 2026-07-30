package com.waker.mission.internal;

import com.waker.mission.MissionDispatch;
import com.waker.mission.MissionHandler;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MissionDispatchConfig {

  @Bean
  MissionDispatch missionDispatch(List<MissionHandler> handlers) {
    return MissionDispatch.fromHandlers(handlers);
  }
}
