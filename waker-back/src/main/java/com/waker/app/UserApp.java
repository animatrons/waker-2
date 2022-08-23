package com.waker.app;

import com.waker.model.User;
import com.waker.model.dto.ResponseDTO;
import com.waker.model.dto.UserDTO;
import com.waker.model.dto.mapper.UserMapperImpl;
import com.waker.model.exception.BusinessErrorCodesAndMessages;
import com.waker.model.exception.BusinessException;
import com.waker.model.exception.TechnicalException;
import com.waker.service.IUserService;
import com.waker.service.impl.UserService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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
    UserMapperImpl userMapper = new UserMapperImpl();

    public ResponseDTO<UserDTO> register(UserDTO userDto) {
        ResponseDTO<UserDTO> response;
        try {
            if (!userDto.validate()) {
                throw new BusinessException(BusinessErrorCodesAndMessages.INVALID_VALUE_IN_FIELDS, "Some fields are either missing or not valid.");
            }
            if (this.userExists(userDto.getEmail())) {
                throw new BusinessException(BusinessErrorCodesAndMessages.ALREADY_EXISTS, "Email is used by one of our clients.");
            }
            User user = userMapper.asEntity(userDto);
            String id = this.userService.addOrUpdate(user);
            userDto.setKey(id);
            response = new ResponseDTO<>(userDto, 201, "User saved");
            log.info("User saved");
        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, e.getCode(), "Error registering user: " + e.getMessage());
        }
        return response;
    }

    private boolean userExists(String email) {
        try {
            List<User> users = userService.search("{}", new Object[] {email}, null, null, 1, 0);
            if (users.size() > 0) return true;
        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
