package com.waker.dao.impl;

import com.waker.dao.AGenericDao;
import com.waker.dao.IReminder;
import com.waker.model.Reminder;

public class ReminderDao extends AGenericDao<Reminder> implements IReminder {
    private ReminderDao() {
        super(Reminder.class);
    }

    private static ReminderDao instance = null;
    public static ReminderDao getInstance() {
        return instance != null ? instance : (instance = new ReminderDao());
    }
}
