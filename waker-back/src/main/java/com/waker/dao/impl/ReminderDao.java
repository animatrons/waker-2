package com.waker.dao.impl;

import com.waker.dao.AGenericDao;
import com.waker.dao.IReminderDao;
import com.waker.model.Reminder;

public class ReminderDao extends AGenericDao<Reminder> implements IReminderDao {
    private ReminderDao() {
        super(Reminder.class);
    }

    private static final ReminderDao instance = new ReminderDao();
    public static ReminderDao getInstance() {
        return instance;
    }
}
