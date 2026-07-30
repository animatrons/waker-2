package com.waker.penalty.internal;

import com.waker.penalty.EmailToContactPenaltyConfig;
import com.waker.penalty.LeaderboardPenaltyConfig;
import com.waker.penalty.PenaltyConfig;
import org.mapstruct.Mapper;
import org.mapstruct.SubclassMapping;

@Mapper(componentModel = "spring")
public interface PenaltyConfigMapper {

  @SubclassMapping(
      source = EmailToContactPenaltyConfig.class,
      target = EmailToContactPenaltyConfig.class)
  @SubclassMapping(source = LeaderboardPenaltyConfig.class, target = LeaderboardPenaltyConfig.class)
  PenaltyConfig copy(PenaltyConfig config);
}
