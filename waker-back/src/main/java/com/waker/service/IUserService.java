package com.waker.service;

import com.waker.model.User;
import com.waker.model.exception.BusinessException;
import com.waker.model.exception.TechnicalException;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public interface IUserService extends IBaseService<User> {

    User getByEmail(String email) throws TechnicalException, BusinessException;
    String createHash(String password) throws TechnicalException;
    boolean validatePassword(String password, String goodHash) throws TechnicalException;
    String buildToken(User use) throws TechnicalException;
    boolean validateToken(String token) throws BusinessException, TechnicalException;
    boolean emailExists(String email) throws BusinessException, TechnicalException;
}
