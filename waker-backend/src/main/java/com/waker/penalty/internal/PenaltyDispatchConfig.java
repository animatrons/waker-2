package com.waker.penalty.internal;

import com.waker.penalty.PenaltyDispatch;
import com.waker.penalty.PenaltyHandler;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PenaltyDispatchConfig {

  @Bean
  PenaltyDispatch penaltyDispatch(List<PenaltyHandler> handlers) {
    return PenaltyDispatch.fromHandlers(handlers);
  }
}
