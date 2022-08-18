package com.waker.app;

import com.waker.model.Reminder;
import com.waker.model.dto.ResponseDTO;
import com.waker.model.exception.BusinessErrorCodesAndMessages;
import com.waker.model.exception.BusinessException;
import com.waker.model.exception.TechnicalException;
import com.waker.service.IReminderService;
import com.waker.service.impl.ReminderService;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;

@Slf4j
public class ReminderApp {

    private static ReminderApp instance = null;
    private ReminderApp() {}
    public static ReminderApp getInstance() {
        if (instance == null)
            instance = new ReminderApp();
        return instance;
    }
    IReminderService reminderService = ReminderService.getInstance();

    public ResponseDTO<Reminder> save(Reminder reminder) {
        ResponseDTO<Reminder> response;

        try {
            if (this.check(reminder)) {
                throw new BusinessException(BusinessErrorCodesAndMessages.INVALID_VALUE_IN_FIELDS, "");
            }
            reminderService.addOrUpdate(reminder);
            response = new ResponseDTO<>(reminder, 200, "Reminder saved");
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
        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, 500, "Server Error getting reminder: " + e.getMessage());
        }
        return response;
    }

    public boolean wasViolated(Reminder reminder) {
        boolean wasViolated = false;
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime deadLine = ZonedDateTime.from(reminder.getDeadline().toInstant()).plusHours(24);
        if (reminder.getStatus() == 0) {
            wasViolated = true;
        }
        if (!now.isAfter(deadLine)) {
            wasViolated = false;
        }
        return wasViolated;
    }

    public ResponseDTO<Reminder> updateStatus(String id, int status) {
        ResponseDTO<Reminder> response;

        try {
            Reminder reminder = reminderService.get(id);
            reminder.setStatus(status);
            if (this.check(reminder)) {
                throw new BusinessException(BusinessErrorCodesAndMessages.INVALID_VALUE_IN_FIELDS, "");
            }
            reminderService.addOrUpdate(reminder);
            response = new ResponseDTO<>(reminder, 200, "Reminder status updated");
        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, 500, "Server Error updating reminder: " + e.getMessage());
        }
        return response;
    }

    private boolean check(Reminder reminder) {
        if (reminder.getStatus() != -1 && reminder.getStatus() != 0 && reminder.getStatus() != 1) {
            return true;
        }
        return false;
    }
}
