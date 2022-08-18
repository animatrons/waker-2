package com.waker.dao.impl;

import com.waker.dao.AGenericDao;
import com.waker.dao.IUserDao;
import com.waker.model.User;

public class UserDao extends AGenericDao<User> implements IUserDao {
    private UserDao() {
        super(User.class);
    }

    private static final UserDao instance = new UserDao();
    public static UserDao getInstance() {
        return instance;
    }
}
