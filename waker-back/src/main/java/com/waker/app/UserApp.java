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
import com.waker.util.Tools;
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

    // Mapper implementations are auto generated after compiling
    UserMapperImpl userMapper = new UserMapperImpl();

    public ResponseDTO<UserDTO> register(UserDTO userDto) {
        ResponseDTO<UserDTO> response;
        try {
            if (!userDto.validate()) {
                throw new BusinessException(BusinessErrorCodesAndMessages.INVALID_VALUE_IN_FIELDS, "Some required fields are either missing or not valid.");
            }
            if (userService.emailExists(userDto.getEmail())) {
                throw new BusinessException(BusinessErrorCodesAndMessages.ALREADY_EXISTS, "Email is used by one of our clients.");
            }
            String hashedPassword = userService.createHash(userDto.getPassword());
            userDto.setPassword(hashedPassword);
            String id = userService.addOrUpdate(
                    userMapper.asEntity(userDto));
            userDto.setKey(id);
            userDto.setPassword("");
            response = new ResponseDTO<>(userDto, 201, "User registered with success");
            log.debug("User saved");
        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, e.getCode(), "Error registering user: " + e.getMessage());
        }
        return response;
    }

    public ResponseDTO<UserDTO> login(UserDTO userDto) {
        ResponseDTO<UserDTO> response;
        try {
            if (!userDto.validateOnLogin()) {
                throw new BusinessException(BusinessErrorCodesAndMessages.INVALID_VALUE_IN_FIELDS, "Some required fields are either missing or not valid.");
            }
            if (!userService.emailExists(userDto.getEmail())) {
                throw new BusinessException(BusinessErrorCodesAndMessages.LOGIN_ERROR, "User is not registered.");
            }
            String email = userDto.getEmail();
            User savedUser = userService.getByEmail(email);
            String passwordHash = savedUser.getPassword();
            boolean isPasswordValid = userService.validatePassword(userDto.getPassword(), passwordHash);
            if (!isPasswordValid) {
                throw new BusinessException(BusinessErrorCodesAndMessages.LOGIN_ERROR, "Invalid password.");
            }
            userDto.setPassword("");
            savedUser.setPassword("");
            String token = userService.buildToken(userMapper.asEntity(userDto));
            userDto.setToken(token);
            response = new ResponseDTO<>(userDto, 200, "Login successful");
            log.debug("User just logged in");
        } catch (BusinessException | TechnicalException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, e.getCode(), "Error login user: " + e.getMessage());
        }
        return response;
    }

    public ResponseDTO<Boolean> validateRequest(String tokenHeader) {
        ResponseDTO<Boolean> response;
        try {
            String token = Tools.resolveToken(tokenHeader);
            boolean isValid = userService.validateToken(token);
            if (isValid) {
                response = new ResponseDTO<>(true, 200, "Access authorized ");
            } else {
                response = new ResponseDTO<>(false, 401, "UNAUTHORIZED REQUEST: invalid token ");
            }
        } catch (BusinessException | TechnicalException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(false, e.getCode(), "UNAUTHORIZED REQUEST: " + e.getMessage());
        }
        return response;
    }

}
