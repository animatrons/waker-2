package com.waker.service.impl;

import com.waker.dao.impl.UserDao;
import com.waker.model.User;
import com.waker.service.IUserService;

public class UserService extends BaseService<User, UserDao> implements IUserService {

    private static IUserService instance = null;
    private UserService() {
        dao = UserDao.getInstance();
    }
    public static IUserService getInstance() {
        if (instance == null)
            instance = new UserService();
        return instance;
    }
}
