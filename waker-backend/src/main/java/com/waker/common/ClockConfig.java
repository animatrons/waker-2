package com.waker.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
public class ClockConfig {

  @Bean
  @Primary
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean("commitmentClock")
  @Profile("!commitment-edit-it & !commitment-sweep-it")
  Clock commitmentClock() {
    return Clock.systemUTC();
  }
}
