package com.waker.app;

import com.waker.model.Reminder;
import com.waker.model.dto.ReminderDTO;
import com.waker.model.dto.mapper.ReminderMapperImpl;
import com.waker.model.dto.ResponseDTO;
import com.waker.model.exception.BusinessErrorCodesAndMessages;
import com.waker.model.exception.BusinessException;
import com.waker.model.exception.TechnicalException;
import com.waker.model.penalty.APenalty;
import com.waker.service.IReminderService;
import com.waker.service.impl.ReminderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.ZonedDateTime;
import java.util.Arrays;

@Slf4j
public class ReminderApp {

    private static ReminderApp instance = null;
    private ReminderApp() {}
    public static ReminderApp getInstance() {
        if (instance == null)
            instance = new ReminderApp();
        return instance;
    }

    ReminderMapperImpl reminderMapper = new ReminderMapperImpl();
    PenaltyApp penaltyApp = PenaltyApp.getInstance();
    IReminderService reminderService = ReminderService.getInstance();

    public ResponseDTO<ReminderDTO> save(ReminderDTO reminderDTO) {
        ResponseDTO<ReminderDTO> response;

        try {
            if (!reminderDTO.validate()) {
                throw new BusinessException(BusinessErrorCodesAndMessages.INVALID_VALUE_IN_FIELDS, "Reminder has not been validated by our " +
                        "validators, please make sure all obligatory fields are correctly filled.");
            }
            Reminder reminder = reminderMapper.asEntity(reminderDTO);
            String id = reminderService.addOrUpdate(reminder);
            reminderDTO.setKey(id);
            response = new ResponseDTO<>(reminderDTO, 200, "Reminder saved");
            log.debug("Reminder saved");
        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, 500, "Server Error saving reminderDTO: " + e.getMessage());
        }
        return response;
    }

    public ResponseDTO<ReminderDTO> get(String id) {
        ResponseDTO<ReminderDTO> response;

        try {
            if (StringUtils.isBlank(id)) {
                throw new BusinessException(BusinessErrorCodesAndMessages.MISSING_REQUIRED_FIELDS, " No id provided");
            }
            Reminder reminder = reminderService.get(id);
            ReminderDTO dto = reminderMapper.asDto(reminder);
            response = new ResponseDTO<>(dto, 200, "Reminder found");
            log.debug("Reminder found");
        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, 500, "Server Error getting reminder: " + e.getMessage());
        }
        return response;
    }

    public ResponseDTO<ReminderDTO> takeAction(boolean toPunish, String id) {
        ResponseDTO<ReminderDTO> reminderResponse = this.get(id);
        ResponseDTO<APenalty> response = new ResponseDTO<>();
        if (reminderResponse.getStatus() == 200) {
            ReminderDTO reminder = reminderResponse.getData();
            boolean isPunishable = toPunish && reminder.getStatus() == 0;
            response = penaltyApp.takeAction(isPunishable, reminder.getPenaltySetting());
            if (isPunishable && response.getStatus() == 200) {
                reminderResponse = this.updateStatus(reminder.getKey(), -1);
                reminder = reminderResponse.getData();
            }
            reminderResponse = new ResponseDTO<>(reminder, response.getStatus(), response.getMessage() + " Status update message: " + reminderResponse.getMessage());
            log.debug(response.getMessage());
        }
        return reminderResponse;
    }

    public ResponseDTO<ReminderDTO> takeAction(boolean toPunish, ReminderDTO reminder) {
        boolean isPunishable = toPunish && reminder.getStatus() == 0;
        ResponseDTO<APenalty> response = penaltyApp.takeAction(isPunishable, reminder.getPenaltySetting());
        if (toPunish && response.getStatus() == 200)
            this.updateStatus(reminder.getKey(), -1);
        log.debug(response.getMessage());
        return new ResponseDTO<>(reminder, reminder.getStatus(), response.getMessage());
    }

    public boolean missed(ReminderDTO reminder) {
        boolean missed = false;
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime deadLine = ZonedDateTime.from(reminder.getDeadline().toInstant()).plusHours(24);
        if (reminder.getStatus() == 0) {
            missed = true;
        }
        if (!now.isAfter(deadLine)) {
            missed = false;
        }
        return missed;
    }

    public ResponseDTO<ReminderDTO> updateStatus(String id, int status) {
        ResponseDTO<ReminderDTO> response;

        try {
            Reminder reminder = reminderService.get(id);
            reminder.setStatus(status);
            ReminderDTO dto = reminderMapper.asDto(reminder);
            if (!dto.validate()) {
                throw new BusinessException(BusinessErrorCodesAndMessages.INVALID_VALUE_IN_FIELDS, "");
            }
            reminderService.addOrUpdate(reminder);
            response = new ResponseDTO<>(dto, 200, "Reminder status updated");
            log.debug("Reminder status updated");
        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, 500, "Server Error updating reminder: " + e.getMessage());
        }
        return response;
    }
}
