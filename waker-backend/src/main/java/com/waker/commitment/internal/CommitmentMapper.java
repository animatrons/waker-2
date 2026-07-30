package com.waker.commitment.internal;

import com.waker.commitment.CommitmentResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface CommitmentMapper {

  CommitmentResponse toResponse(Commitment commitment);
}
