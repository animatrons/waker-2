package com.waker.model.dto.mapper;

import java.util.List;

public interface IMapper<M, D> {

    D asDto(M entity);
    List<D> asDtos(List<M> entities);
    M asEntity(D dto);
    List<M> asEntities(List<D> dtos);
}
