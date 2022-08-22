package com.waker.app;

import com.waker.model.User;
import com.waker.model.dto.ResponseDTO;
import com.waker.model.exception.BusinessException;
import com.waker.model.exception.TechnicalException;
import com.waker.service.IUserService;
import com.waker.service.impl.UserService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserApp {

    private static UserApp instance = null;
    private UserApp() {}
    public static UserApp getInstance() {
        if (instance == null)
            instance = new UserApp();
        return instance;
    }
    IUserService userService = UserService.getInstance();

    public ResponseDTO<User> register(User user) {
        ResponseDTO<User> response;
        try {
            String id = this.userService.addOrUpdate(user);
            response = new ResponseDTO<>(user, 200, "User saved");
            log.info("User saved");
        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, 500, "Server Error adding new user: " + e.getMessage());
        }
        return response;
    }
}
