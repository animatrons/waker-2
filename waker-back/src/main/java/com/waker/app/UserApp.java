package com.waker.app;

import com.waker.model.User;
import com.waker.model.dto.MailDTO;
import com.waker.model.dto.ResponseDTO;
import com.waker.model.dto.UserDTO;
import com.waker.model.dto.UserOutputDTO;
import com.waker.model.dto.mapper.UserMapperImpl;
import com.waker.model.dto.mapper.UserOutputMapperImpl;
import com.waker.model.exception.BusinessErrorCodesAndMessages;
import com.waker.model.exception.BusinessException;
import com.waker.model.exception.TechnicalException;
import com.waker.service.IMailServiceProvider;
import com.waker.service.ITemplatingService;
import com.waker.service.IUserService;
import com.waker.service.impl.GmailApiService;
import com.waker.service.impl.HandlebarsTemplatingService;
import com.waker.service.impl.MailSlurpService;
import com.waker.service.impl.UserService;
import com.waker.util.Tools;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    UserOutputMapperImpl outputMapper = new UserOutputMapperImpl();
    IMailServiceProvider mailService = GmailApiService.getInstance();
    ITemplatingService templatingService = HandlebarsTemplatingService.getInstance();

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
            String id = userService.addOrUpdate(userMapper.asEntity(userDto));
            userDto.setKey(id);
            userDto.setPassword("");
            response = new ResponseDTO<>(userDto, 201, "User registered with success");

            Map<String, UserDTO> tempMap = new HashMap<>();
            tempMap.put("user", userDto);
            String html = templatingService.render(tempMap, "email_templates/user_registration_confirmation_email.hbs");
            MailDTO mail = new MailDTO(mailService.getMainEmail(), "Waker Team",
                    userDto.getEmail(), userDto.getFirstName() + " " + userDto.getLastName(), "Registration", "", html);
            ResponseDTO<MailDTO> mailStatusResponse = mailService.send(mail, true);
            // TODO: send email verification when user registers

        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage());
            response = new ResponseDTO<>(null, e.getCode(), "Error registering user: " + e.getMessage());
        }
        return response;
    }

    public ResponseDTO<UserOutputDTO> login(UserDTO userDto) {
        ResponseDTO<UserOutputDTO> response;
        try {
            if (!userDto.validate("login")) {
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
            String token = userService.buildToken(userMapper.asEntity(userDto));
            UserOutputDTO outputDTO = outputMapper.asDto(userDto);
            outputDTO.setAccessToken(token);
            response = new ResponseDTO<>(outputDTO, 200, "Login successful");
        } catch (BusinessException | TechnicalException e) {
            log.error(e.getMessage());
            response = new ResponseDTO<>(null, e.getCode(), "Error login user: " + e.getMessage());
        }
        return response;
    }

    public ResponseDTO<String> validateRequest(String tokenHeader) {
        ResponseDTO<String> response;
        try {
            String token = Tools.resolveToken(tokenHeader);
            boolean isValid = userService.validateToken(token);
            if (isValid) {
                String email = userService.getSubjectFromToken(token);
                response = new ResponseDTO<>(email, 200, "Access authorized ");
            } else {
                response = new ResponseDTO<>(null, 401, "UNAUTHORIZED REQUEST: invalid token ");
            }
        } catch (TechnicalException | BusinessException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, e.getCode(), "A technical error occurred while validating the token: " + e.getMessage());
        }
        return response;
    }

}
