package com.waker.dao.impl;

import com.waker.dao.AGenericDao;
import com.waker.dao.IUser;
import com.waker.model.User;

public class UserDao extends AGenericDao<User> implements IUser {
    private UserDao() {
        super(User.class);
    }

    private static UserDao instance = null;
    public static UserDao getInstance() {
        return instance != null ? instance : (instance = new UserDao());
    }
}
