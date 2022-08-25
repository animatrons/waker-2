package com.waker.model.dto.mapper;

import com.waker.model.dto.UserDTO;
import com.waker.model.dto.UserOutputDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserOutputMapper extends IMapper<UserDTO, UserOutputDTO> {

    @Override
    @Mapping(source = "email", target = "subject")
    UserOutputDTO asDto(UserDTO entity);

    @Override
    @Mapping(source = "subject", target = "email")
    UserDTO asEntity(UserOutputDTO dto);
}
