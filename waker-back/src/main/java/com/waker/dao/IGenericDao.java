package com.waker.dao;

import com.waker.model.AModel;
import com.waker.model.exception.TechnicalException;

import java.util.List;

public interface IGenericDao<T extends AModel> {

    T find(String id) throws TechnicalException;
    List<T> find(String query, Object[] parameters, String projection, String sort, int pageSize, int start)
            throws TechnicalException;
    void delete(String id) throws TechnicalException;
    void delete(T obj) throws TechnicalException;
    void delete(List<T> objs) throws TechnicalException;
    String addOrUpdate(T obj) throws TechnicalException;
    List<String> addOrUpdate(List<T> objs) throws TechnicalException;
    long count(String query) throws TechnicalException;

}
