package com.waker.model.dto.mapper;

import com.waker.model.Reminder;
import com.waker.model.dto.ReminderDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReminderMapper extends IMapper<Reminder, ReminderDTO> {

    ReminderMapper INSTANCE = Mappers.getMapper(ReminderMapper.class);
}
