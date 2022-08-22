package com.waker.model.dto.mapper;

import org.mapstruct.Mapper;
import com.waker.model.User;
import com.waker.model.dto.UserDTO;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper extends IMapper<User, UserDTO> {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
}
