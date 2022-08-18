package com.waker.service.impl;

import com.waker.dao.IReminderDao;
import com.waker.dao.impl.ReminderDao;
import com.waker.model.Reminder;
import com.waker.service.IReminderService;

public class ReminderService extends BaseService<Reminder, IReminderDao> implements IReminderService {

    private static IReminderService instance = null;
    private ReminderService() {
        dao = ReminderDao.getInstance();
    }
    public static IReminderService getInstance() {
        if (instance == null)
            instance = new ReminderService();
        return instance;
    }
}
