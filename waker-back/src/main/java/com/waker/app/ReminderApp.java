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

    public ResponseDTO<ReminderDTO> save(ReminderDTO reminderDTO, String loggedInUsersEmail) {
        ResponseDTO<ReminderDTO> response;

        try {
            // TODO: validate reminder's user by simply checking its email
            if (!reminderDTO.validate()) {
                throw new BusinessException(BusinessErrorCodesAndMessages.INVALID_VALUE_IN_FIELDS, "Reminder in invalid please make sure all obligatory fields are correctly filled.");
            }
            if (!isOperationAllowed(reminderDTO, loggedInUsersEmail)) {
                throw new BusinessException(BusinessErrorCodesAndMessages.UNAUTHORIZED, " Write operation not authorized, cannot add a reminder on behalf of other users.");
            }
            Reminder reminder = reminderMapper.asEntity(reminderDTO);
            String id = reminderService.addOrUpdate(reminder);
            reminderDTO.setKey(id);
            response = new ResponseDTO<>(reminderDTO, 200, "Reminder saved");
        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, e.getCode(), "Server Error saving reminderDTO: " + e.getMessage());
        }
        return response;
    }

    public ResponseDTO<ReminderDTO> get(String id, String loggedInUsersEmail) {
        ResponseDTO<ReminderDTO> response;
        try {
            if (StringUtils.isBlank(id)) {
                throw new BusinessException(BusinessErrorCodesAndMessages.MISSING_REQUIRED_FIELDS, " No id provided");
            }
            Reminder reminder = reminderService.get(id);
            ReminderDTO dto = reminderMapper.asDto(reminder);
            if (!isOperationAllowed(dto, loggedInUsersEmail)) {
                throw new BusinessException(BusinessErrorCodesAndMessages.UNAUTHORIZED, " Read operation not authorized, the Reminder doc does not belong to the current logged in user.");
            }
            response = new ResponseDTO<>(dto, 200, "Reminder found");
        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, e.getCode(), "Server Error getting reminder: " + e.getMessage());
        }
        return response;
    }

    public ResponseDTO<ReminderDTO> takeAction(boolean toPunish, String id, String loggedInUsersEmail) {
        ResponseDTO<ReminderDTO> reminderResponse = this.get(id, loggedInUsersEmail);
        ResponseDTO<APenalty> response = new ResponseDTO<>();
        if (reminderResponse.getStatus() == 200) {
            ReminderDTO reminder = reminderResponse.getData();
            boolean isPunishable = toPunish && reminder.getStatus() == 0;
            response = penaltyApp.takeAction(isPunishable, reminder.getPenaltySetting());
            if (isPunishable && response.getStatus() == 200) {
                reminderResponse = this.updateStatus(reminder.getKey(), -1, loggedInUsersEmail);
                reminder = reminderResponse.getData();
            }
            reminderResponse = new ResponseDTO<>(reminder, response.getStatus(), response.getMessage() + " Status update message: " + reminderResponse.getMessage());
        }
        return reminderResponse;
    }

    public ResponseDTO<ReminderDTO> takeAction(boolean toPunish, ReminderDTO reminder, String loggedInUsersEmail) {
        ResponseDTO<ReminderDTO> reminderResponse;

        if (isOperationAllowed(reminder, loggedInUsersEmail)) {
            boolean isPunishable = toPunish && reminder.getStatus() == 0;
            ResponseDTO<APenalty> response = penaltyApp.takeAction(isPunishable, reminder.getPenaltySetting());
            if (toPunish && response.getStatus() == 200)
                this.updateStatus(reminder.getKey(), -1, loggedInUsersEmail);
            reminderResponse = new ResponseDTO<>(reminder, reminder.getStatus(), response.getMessage());
        } else {
            reminderResponse = new ResponseDTO<>(null, 401, "Operation not authorized, cannot punish a user as another user.");
        }

        return reminderResponse;
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

    public ResponseDTO<ReminderDTO> updateStatus(String id, int status, String loggedInUsersEmail) {
        ResponseDTO<ReminderDTO> response;
        try {
            Reminder reminder = reminderService.get(id);
            reminder.setStatus(status);
            ReminderDTO dto = reminderMapper.asDto(reminder);
            if (!dto.validate()) {
                throw new BusinessException(BusinessErrorCodesAndMessages.INVALID_VALUE_IN_FIELDS, "Reminder status code is invalid");
            }
            if (!isOperationAllowed(dto, loggedInUsersEmail)) {
                throw new BusinessException(BusinessErrorCodesAndMessages.UNAUTHORIZED, "Write operation not authorized, cannot update a reminder on behalf of other users.");
            }
            reminderService.addOrUpdate(reminder);
            response = new ResponseDTO<>(dto, 200, "Reminder status updated");
        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, e.getCode(), "Server Error updating reminder: " + e.getMessage());
        }
        return response;
    }

    /**
     * Verifies if the reminder's user is the one logged in with the email address passed as a parameter
     * IMPORTANT: use only after validating the reminder dto
     * @param reminderDto reminder dto, MUST BE VALIDATED
     * @param loggedInUsersEmail authenticated user's email address
     * @return boolean result
     */
    private boolean isOperationAllowed(ReminderDTO reminderDto, String loggedInUsersEmail) {
        return reminderDto.getUser().getEmail().equals(loggedInUsersEmail);
    }

}
