package com.waker.app;

import com.waker.model.Reminder;
import com.waker.model.dto.ResponseDTO;
import com.waker.model.exception.BusinessErrorCodesAndMessages;
import com.waker.model.exception.BusinessException;
import com.waker.model.exception.TechnicalException;
import com.waker.model.penalty.APenalty;
import com.waker.model.penalty.Penalties;
import com.waker.service.IReminderService;
import com.waker.service.impl.ReminderService;
import lombok.extern.slf4j.Slf4j;

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

    PenaltyApp penaltyApp = PenaltyApp.getInstance();
    IReminderService reminderService = ReminderService.getInstance();

    public ResponseDTO<Reminder> save(Reminder reminder) {
        ResponseDTO<Reminder> response;

        try {
            if (!this.isValid(reminder)) {
                throw new BusinessException(BusinessErrorCodesAndMessages.INVALID_VALUE_IN_FIELDS, "");
            }
            String id = reminderService.addOrUpdate(reminder);
            reminder.setKey(id);
            response = new ResponseDTO<>(reminder, 200, "Reminder saved");
            log.info("Reminder saved");
        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, 500, "Server Error saving reminder: " + e.getMessage());
        }
        return response;
    }

    public ResponseDTO<Reminder> get(String id) {
        ResponseDTO<Reminder> response;

        try {
            Reminder reminder = reminderService.get(id);
            response = new ResponseDTO<>(reminder, 200, "Reminder found");
            log.info("Reminder found");
        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, 500, "Server Error getting reminder: " + e.getMessage());
        }
        return response;
    }

    public ResponseDTO<Reminder> takeAction(boolean toPunish, String id) {
        ResponseDTO<Reminder> reminderResponse = this.get(id);
        ResponseDTO<APenalty> response = new ResponseDTO<>();
        if (reminderResponse.getStatus() == 200) {
            Reminder reminder = reminderResponse.getData();
            boolean isPunishable = toPunish && reminder.getStatus() == 0;
            response = penaltyApp.takeAction(isPunishable, reminder.getPenaltySetting());
            if (isPunishable && response.getStatus() == 200) {
                reminderResponse = this.updateStatus(reminder.getKey(), -1);
                reminder = reminderResponse.getData();
            }
            reminderResponse = new ResponseDTO<>(reminder, response.getStatus(), response.getMessage());
            log.info(response.getMessage());
        }
        return reminderResponse;
    }

    public ResponseDTO<Reminder> takeAction(boolean toPunish, Reminder reminder) {
        boolean isPunishable = toPunish && reminder.getStatus() == 0;
        ResponseDTO<APenalty> response = penaltyApp.takeAction(isPunishable, reminder.getPenaltySetting());
        if (toPunish && response.getStatus() == 200)
            this.updateStatus(reminder.getKey(), -1);
        log.info(response.getMessage());
        return new ResponseDTO<>(reminder, reminder.getStatus(), response.getMessage());
    }

    public boolean missed(Reminder reminder) {
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

    public ResponseDTO<Reminder> updateStatus(String id, int status) {
        ResponseDTO<Reminder> response;

        try {
            Reminder reminder = reminderService.get(id);
            reminder.setStatus(status);
            if (!this.isValid(reminder)) {
                throw new BusinessException(BusinessErrorCodesAndMessages.INVALID_VALUE_IN_FIELDS, "");
            }
            reminderService.addOrUpdate(reminder);
            response = new ResponseDTO<>(reminder, 200, "Reminder status updated");
            log.info("Reminder status updated");
        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, 500, "Server Error updating reminder: " + e.getMessage());
        }
        return response;
    }

    private boolean isValid(Reminder reminder) {
        if (reminder.getStatus() != -1 && reminder.getStatus() != 0 && reminder.getStatus() != 1) {
            return false;
        }
        String aClass = reminder.getPenaltySetting().get_class();
        if (Arrays.stream(Penalties.values()).noneMatch(penalty -> penalty.toString().equals(aClass))) {
            return false;
        }
        return true;
    }
}
