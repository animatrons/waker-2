package com.waker.service;

import com.waker.model.AModel;
import com.waker.model.exception.BusinessException;
import com.waker.model.exception.TechnicalException;

import java.util.List;

public interface IBaseService<T extends AModel> {

    String addOrUpdate(T obj) throws TechnicalException, BusinessException;
    List<String> addOrUpdate(List<T> objects) throws TechnicalException, BusinessException;
    T get(String id) throws TechnicalException, BusinessException;
    List<T> search(String query, Object[] parameters, String projection, String sort, int pageSize, int start) throws TechnicalException, BusinessException;
    void delete(String id) throws TechnicalException, BusinessException;
    void delete(List<T> objects) throws TechnicalException, BusinessException;
    long count(String query) throws TechnicalException, BusinessException;

}
